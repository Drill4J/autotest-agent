@file:Suppress("MemberVisibilityCanBePrivate")

package com.epam.drill.auto.test.agent

import com.epam.drill.auto.test.agent.ThreadStorage.storage
import com.epam.drill.auto.test.agent.penetration.StrategyManager.process
import javassist.*
import java.io.*

object AgentClassTransformer {

    private val pool = ClassPool.getDefault()

    const val CLASS_NAME = "com.epam.drill.auto.test.agent.AgentClassTransformer"

    @Suppress("unused")
    fun memorizeTestName(testName: String?) {
        storage.set(testName)
        memorizeTestNameNative(testName)
    }

    external fun memorizeTestNameNative(testName: String?)

    @Suppress("unused")
    external fun sessionId(): String?

    @Suppress("unused")
    fun transform(className: String, classBytes: ByteArray): ByteArray? {
        return try {
            getCtClass(className, classBytes)?.let { insertTestNames(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun insertTestNames(ctClass: CtClass): ByteArray? {
        var result: ByteArray? = null
        try {
            result = process(ctClass)
        } catch (ignored: CannotCompileException) {
            ignored.printStackTrace()
        } catch (ignored: IOException) {
            ignored.printStackTrace()
        } catch (ignored: NotFoundException) {
            ignored.printStackTrace()
        }
        return result
    }

    private fun getCtClass(className: String, classBytes: ByteArray): CtClass? {
        var ctClass: CtClass? = null
        try {
            pool.insertClassPath(ByteArrayClassPath(className, classBytes))
            ctClass = pool[formatClassName(className)]
        } catch (ignored: NotFoundException) {
        }
        return ctClass
    }

    private fun formatClassName(className: String): String {
        return className.replace("/", ".")
    }
}