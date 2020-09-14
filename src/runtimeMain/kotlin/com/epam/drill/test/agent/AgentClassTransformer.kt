@file:Suppress("MemberVisibilityCanBePrivate")

package com.epam.drill.test.agent

import com.epam.drill.kni.*
import com.epam.drill.test.agent.instrumentation.StrategyManager.process
import com.epam.drill.logger.*
import javassist.*
import java.security.ProtectionDomain

@Kni
actual object AgentClassTransformer {
    private val logger = Logging.logger(AgentClassTransformer::class.java.name)

    private val pool = ClassPool.getDefault()

    const val CLASS_NAME = "AgentClassTransformer"

    actual fun transform(className: String, classBytes: ByteArray, loader: Any?, protectionDomain: Any?): ByteArray? =
        try {
            when (className) {
                "io/netty/util/internal/logging/Log4J2Logger"-> null
                else -> getCtClass(className, classBytes)?.let {
                    insertTestNames(
                        it,
                        loader as? ClassLoader,
                        protectionDomain as? ProtectionDomain
                    )
                }
            }
        } catch (e: Exception) {
            null
        }

    private fun insertTestNames(
        ctClass: CtClass,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? = try {
        process(ctClass, classLoader, protectionDomain)
    } catch (ex: Exception) {
        logger.warn(ex) { "Can't instrument '${ctClass.name}' class." }
        null
    }

    private fun getCtClass(className: String, classBytes: ByteArray): CtClass? {
        pool.insertClassPath(ByteArrayClassPath(className, classBytes))
        return pool[formatClassName(className)]
    }

    private fun formatClassName(className: String): String {
        return className.replace("/", ".")
    }
}
