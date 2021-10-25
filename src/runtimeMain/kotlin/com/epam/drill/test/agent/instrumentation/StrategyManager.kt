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
import org.objectweb.asm.*
import java.io.*
import java.util.jar.*

@Kni
actual object StrategyManager {
    private val logger = Logging.logger(StrategyManager::class.java.name)

    internal var allStrategies: MutableMap<String, MutableSet<TransformStrategy>> = mutableMapOf()
    private var strategies: MutableSet<TransformStrategy> = HashSet()
    private var systemStrategies: MutableSet<TransformStrategy> = HashSet()

    init {
        systemStrategies.add(
            //TODO EPMDJ-8916 Use default realisation
            object : OkHttpClient() {
                override fun permit(classReader: ClassReader): Boolean {
                    return classReader.interfaces.any { "drill/$it" == "okhttp3/internal/http/HttpCodec" }
                }
            })
        systemStrategies.add(ApacheClient())
        systemStrategies.add(JavaHttpUrlConnection())
        systemStrategies.add(Selenium())
        systemStrategies.add(Kafka())
        ClientsCallback.initRequestCallback {
            val headers = mutableMapOf<String, String>()
            ThreadStorage.sessionId()?.let {
                headers.put(SESSION_ID_HEADER, it)
            }
            ThreadStorage.storage.get()?.let {
                headers.put(TEST_NAME_HEADER, it)
            }
            headers
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
        for (strategy in strategies) {
            if (strategy.permit(classReader)) {
                transformedClassBytes.add(strategy.transform(className, classBytes, loader, protectionDomain))
            }
        }
        return transformedClassBytes.firstOrNull { it != null }
    }
}
