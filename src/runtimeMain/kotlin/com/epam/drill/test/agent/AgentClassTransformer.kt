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
@file:Suppress("MemberVisibilityCanBePrivate")

package com.epam.drill.test.agent

import com.epam.drill.kni.*
import com.epam.drill.test.agent.instrumentation.StrategyManager.process
import com.epam.drill.logger.*
import javassist.*
import java.io.ByteArrayInputStream
import java.security.ProtectionDomain

@Kni
actual object AgentClassTransformer {
    private val logger = Logging.logger(AgentClassTransformer::class.java.name)

    private val debug: String? = System.getProperty("drill.debug")

    const val CLASS_NAME = "AgentClassTransformer"

    actual fun transform(className: String, classBytes: ByteArray, loader: Any?, protectionDomain: Any?): ByteArray? =
        try {
            when (className) {
                "io/netty/util/internal/logging/Log4J2Logger" -> null
                else -> getCtClass(classBytes, loader as? ClassLoader)?.let { (pool, it)->
                    insertTestNames(
                        it,
                        pool,
                        loader as? ClassLoader,
                        protectionDomain as? ProtectionDomain
                    )
                }
            }
        } catch (e: Exception) {
            if (debug == "true" && className.startsWith("org.junit.runner.notification.RunNotifier")) {
                e.printStackTrace()
            }

            null
        }

    private fun insertTestNames(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? = try {
        process(ctClass, pool, classLoader, protectionDomain)
    } catch (ex: Exception) {
        logger.warn(ex) { "Can't instrument '${ctClass.name}' class." }
        null
    }

    private fun getCtClass( classBytes: ByteArray, loader: ClassLoader?): Pair<ClassPool, CtClass> {
        val classPool = ClassPool(true)
        if (loader == null) {
            classPool.appendClassPath(LoaderClassPath(ClassLoader.getSystemClassLoader()))
        } else {
            classPool.appendClassPath(LoaderClassPath(loader))
        }

        val clazz = classPool.makeClass(ByteArrayInputStream(classBytes), false)
        clazz.defrost()

        return classPool to clazz
    }

}
