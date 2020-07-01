package com.epam.drill.auto.test.agent.penetration

import com.epam.drill.auto.test.agent.penetration.http.apache.*
import com.epam.drill.auto.test.agent.penetration.http.java.*
import com.epam.drill.auto.test.agent.penetration.http.ok.*
import com.epam.drill.auto.test.agent.penetration.testing.jmeter.JMeterPenetration
import com.epam.drill.auto.test.agent.penetration.testing.junit.JUnitPenetration
import com.epam.drill.auto.test.agent.penetration.testing.junit.JUnitRunnerPenetration
import com.epam.drill.auto.test.agent.penetration.testing.testng.TestNGPenetration
import javassist.CannotCompileException
import javassist.CtClass
import javassist.NotFoundException
import java.io.IOException
import java.util.*

object StrategyManager {
    private const val JUNIT = "junit"
    private const val JMETER = "jmeter"
    private const val TESTNG = "testng"
    var strategies: MutableSet<Strategy> = HashSet()

    fun initialize(rawFrameworkPlugins: String) {
        val plugins = rawFrameworkPlugins.split(";".toRegex()).toTypedArray()
        for (plugin in plugins) {
            matchStrategy(plugin)
        }
        if (strategies.isEmpty()) {
            enableAllStrategies()
        }
    }

    @JvmStatic
    @Throws(NotFoundException::class, CannotCompileException::class, IOException::class)
    fun process(ctClass: CtClass): ByteArray? {
        for (strategy in strategies) {
            if (strategy.permit(ctClass)) return strategy.instrument(ctClass)
        }
        return null
    }

    private fun matchStrategy(alias: String) {
        when (alias) {
            JUNIT -> {
                strategies.add(JUnitPenetration())
            }
            JMETER -> {
                strategies.add(JMeterPenetration())
            }
            TESTNG -> {
                strategies.add(TestNGPenetration())
            }
        }
    }

    private fun enableAllStrategies() {
        strategies.add(OkHttpClient())
        strategies.add(ApacheClient())
        strategies.add(JavaHttpUrlConnection())
        strategies.add(JUnitPenetration())
        strategies.add(JUnitRunnerPenetration())
        strategies.add(JMeterPenetration())
        strategies.add(TestNGPenetration())
    }
}