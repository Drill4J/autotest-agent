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
package com.epam.drill.test.agent.instrument.strategy

import java.io.ByteArrayInputStream
import java.security.ProtectionDomain
import javassist.ClassPool
import javassist.CtClass
import javassist.LoaderClassPath
import mu.KotlinLogging
import com.epam.drill.agent.instrument.Transformer
import com.epam.drill.test.agent.instrument.StrategyManager

@Suppress("LeakingThis")
abstract class AbstractTestStrategy : Transformer {

    init {
        StrategyManager.allStrategies[id] =
            (StrategyManager.allStrategies[id] ?: mutableSetOf()).apply { add(this@AbstractTestStrategy) }
    }

    private val logger = KotlinLogging.logger {}

    abstract val id: String

    override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?
    ): ByteArray = ClassPool(true).run {
        val classLoader = loader ?: ClassLoader.getSystemClassLoader()
        this.appendClassPath(LoaderClassPath(classLoader as? ClassLoader))
        this.makeClass(ByteArrayInputStream(classFileBuffer), false).let {
            val logError: (Throwable) -> Unit = { e ->
                logger.error(e) { "transform: Error during instrumenting, class=${it.name}" }
            }
            val instrument: (CtClass) -> ByteArray? = { ctClass ->
                instrument(ctClass, this, loader as? ClassLoader, protectionDomain as? ProtectionDomain)
            }
            it.defrost()
            it.runCatching(instrument).onFailure(logError).getOrNull() ?: classFileBuffer
        }
    }

    abstract fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray?

}
