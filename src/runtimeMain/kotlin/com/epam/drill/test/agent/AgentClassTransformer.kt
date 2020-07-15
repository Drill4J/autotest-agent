@file:Suppress("MemberVisibilityCanBePrivate")

package com.epam.drill.test.agent

import com.epam.drill.test.agent.penetration.StrategyManager.process
import com.epam.drill.logger.*
import javassist.*

object AgentClassTransformer {
    private val logger = Logging.logger(AgentClassTransformer::class.java.name)

    private val pool = ClassPool.getDefault()

    const val CLASS_NAME = "AgentClassTransformer"

    @Suppress("unused")
    fun transform(className: String, classBytes: ByteArray): ByteArray? = try {
        getCtClass(className, classBytes)?.let { insertTestNames(it) }
    } catch (e: Exception) {
        null
    }

    private fun insertTestNames(ctClass: CtClass): ByteArray? = try {
        process(ctClass)
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
