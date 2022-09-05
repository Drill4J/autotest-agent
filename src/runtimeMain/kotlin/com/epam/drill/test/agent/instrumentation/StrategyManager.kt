/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.test.agent.instrumentation

import com.epam.drill.agent.instrument.*
import com.epam.drill.agent.instrument.http.apache.*
import com.epam.drill.agent.instrument.http.java.*
import com.epam.drill.agent.instrument.http.ok.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.http.selenium.*
import com.epam.drill.test.agent.instrumentation.kafka.*
import com.epam.drill.test.agent.instrumentation.runners.*
import javassist.*
import org.objectweb.asm.*
import java.io.*
import java.security.*
import java.util.jar.*
import com.epam.drill.agent.instrument.util.*

@Kni
actual object StrategyManager {
    private val logger = Logging.logger(StrategyManager::class.java.name)

    internal var allStrategies: MutableMap<String, MutableSet<TransformStrategy>> = mutableMapOf()
    private var strategies: MutableSet<TransformStrategy> = HashSet()
    private var systemStrategies: MutableSet<TransformStrategy> = HashSet()

    init {
        systemStrategies.add(OkHttpClientStub)
        systemStrategies.add(ApacheClient)
        systemStrategies.add(JavaHttpUrlConnection)
        systemStrategies.add(Selenium)
        systemStrategies.add(Kafka)
        ClientsCallback.initRequestCallback {
            mutableMapOf<String, String>().apply {
                ThreadStorage.sessionId()?.let {
                    put(SESSION_ID_HEADER, it)
                }
                ThreadStorage.storage.get()?.let {
                    put(TEST_ID_HEADER, it)
                }
            }
        }

        ClientsCallback.initSendConditionCallback {
            ClientsCallback.getHeaders().run {
                isNotEmpty() && get(SESSION_ID_HEADER) != null && get(TEST_ID_HEADER) != null
            }
        }
    }

    actual fun initialize(rawFrameworkPlugins: String, isManuallyControlled: Boolean) {
        hotLoad()
        val plugins = rawFrameworkPlugins.split(";".toRegex()).toTypedArray()
        strategies.addAll(allStrategies.filterKeys { plugins.contains(it) }.values.flatten())
        if (strategies.isEmpty()) {
            strategies.addAll(allStrategies.values.flatten())
        }
        if (isManuallyControlled) strategies.add(JunitRunner())
        strategies.addAll(systemStrategies)
        logger.debug { "Added strategies: ${strategies.map { it::class.simpleName }.joinToString()}" }
    }

    // TODO EPMDJ-10496 path encode with url encoding
    private fun hotLoad() {
        val pack = this::class.java.`package`.name.replace(".", "/")
        val removeSuffix = this::class.java.getResource("/$pack").file
            .removePrefix("file:")
            .removeSuffix("!/$pack")
        JarFile(File(removeSuffix)).use {
            it.entries().iterator().forEach {
                val name = it.name
                if (name.startsWith(pack) && name.endsWith(".class")) {
                    val replace = name.replace("/", ".").removeSuffix(".class")
                    runCatching { Class.forName(replace) }.onFailure { throwable ->
                        logger.error(throwable) { "Error while loading $replace class" }
                    }
                }
            }
        }
    }

    internal fun process(
        className: String,
        classBytes: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? {
        val transformedClassBytes = mutableListOf<ByteArray?>()
        val classReader = ClassReader(classBytes)
        logger.debug { "Processsing ${classReader.className}" }
        for (strategy in strategies) {
            if (strategy.permit(classReader)) {
                transformedClassBytes.add(strategy.transform(className, classBytes, loader, protectionDomain))
            }
        }
        return transformedClassBytes.firstOrNull { it != null }
    }
}
//TODO EPMDJ-8916 Replace with [com.epam.drill.agent.instrument.http.ok.OkHttpClient]
object OkHttpClientStub : TransformStrategy() {
    override fun permit(classReader: ClassReader): Boolean {
        return classReader.interfaces.any { "$it" == "okhttp3/internal/http/HttpCodec" || "$it" == "okhttp3/internal/http/ExchangeCodec" }
    }
    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {

        kotlin.runCatching {
            ctClass.getDeclaredMethod("writeRequestHeaders").insertBefore(
                """
                java.lang.System.out.println("CALLED writeRequestHeaders");
                if (${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::isSendCondition.name}()) {
                    java.lang.System.out.println("CALLED writeRequestHeaders passed condition");
                    okhttp3.Request.Builder builder = $1.newBuilder();
                    java.util.Map headers = ${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::getHeaders.name}();
                    java.util.Iterator iterator = headers.entrySet().iterator();             
                    while (iterator.hasNext()) {
                        java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
                        builder.addHeader((String) entry.getKey(), (String) entry.getValue());
                    }
                    $1 = builder.build();
                    ${Log::class.java.name}.INSTANCE.${Log::injectHeaderLog.name}(headers);                    
                } else {
                  java.lang.System.out.println("CALLED writeRequestHeaders failed condition ${ClientsCallback::class.qualifiedName}");
                }
            """.trimIndent()
            )
            ctClass.getDeclaredMethod("openResponseBodySource").insertBefore(
                """
                if (${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::isResponseCallbackSet.name}()) {
                    java.util.Map allHeaders = new java.util.HashMap();
                    java.util.Iterator iterator = $1.headers().names().iterator();
                    while (iterator.hasNext()) { 
                        String key = (String) iterator.next();
                        String value = $1.headers().get(key);
                        allHeaders.put(key, value);
                    }
                    ${ClientsCallback::class.qualifiedName}.INSTANCE.${ClientsCallback::storeHeaders.name}(allHeaders);
                }
                """.trimIndent()
            )
        }.onFailure {
            logger.error(it) { "Error while instrumenting the class ${ctClass.name}" }
        }

        return ctClass.toBytecode()
    }
}
