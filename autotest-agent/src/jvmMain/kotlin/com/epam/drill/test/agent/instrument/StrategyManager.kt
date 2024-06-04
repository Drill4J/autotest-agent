/**
 * Copyright 2020 - 2022 EPAM Systems
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
package com.epam.drill.test.agent.instrument

import com.epam.drill.agent.instrument.*
import com.epam.drill.test.agent.instrument.clients.*
import com.epam.drill.test.agent.instrument.servers.ReactorTransformer
import com.epam.drill.test.agent.instrument.strategy.selenium.*
import com.epam.drill.test.agent.instrument.strategy.kafka.*
import com.epam.drill.test.agent.instrument.strategy.runner.*
import org.objectweb.asm.*
import java.io.*
import java.util.jar.*
import mu.KotlinLogging

actual object StrategyManager {

    private val logger = KotlinLogging.logger {}

    internal var allStrategies: MutableMap<String, MutableSet<Transformer>> = mutableMapOf()
    private var strategies: MutableSet<Transformer> = HashSet()
    private var systemStrategies: MutableSet<Transformer> = HashSet()

    init {
        systemStrategies.add(ReactorTransformer)
        systemStrategies.add(OkHttpClientStub)
        systemStrategies.add(ApacheHttpClientTransformer)
        systemStrategies.add(JavaHttpClientTransformer)
        systemStrategies.add(Selenium)
        systemStrategies.add(Kafka)
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
        for (strategy in strategies) {
            if (strategy.permit(classReader.className, classReader.superName, classReader.interfaces)) {
                transformedClassBytes.add(strategy.transform(className, classBytes, loader, protectionDomain))
            }
        }
        return transformedClassBytes.firstOrNull { it != null && !it.contentEquals(classBytes) }
    }
}

//TODO EPMDJ-8916 Replace with [com.epam.drill.agent.instrument.http.ok.OkHttpClient]
private object OkHttpClientStub : Transformer {
    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        //todo EPMDJ-10494 no need drill suffix after removing dependency
        return interfaces.any { "drill/$it" == "okhttp3/internal/http/HttpCodec" }
    }
    override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?
    ) = OkHttp3ClientTransformer.transform(className, classFileBuffer, loader, protectionDomain)
}
