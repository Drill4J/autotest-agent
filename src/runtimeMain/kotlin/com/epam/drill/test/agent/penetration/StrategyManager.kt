package com.epam.drill.test.agent.penetration

import com.epam.drill.test.agent.penetration.http.apache.*
import com.epam.drill.test.agent.penetration.http.java.*
import com.epam.drill.test.agent.penetration.http.ok.*
import com.epam.drill.test.agent.penetration.http.selenium.*
import com.epam.drill.test.agent.penetration.testing.jmeter.*
import com.epam.drill.test.agent.penetration.testing.junit.*
import com.epam.drill.test.agent.penetration.testing.testng.*
import javassist.*
import java.io.*
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
        strategies.add(Selenium())
        strategies.add(OkHttpClient())
        strategies.add(ApacheClient())
        strategies.add(JavaHttpUrlConnection())
        strategies.add(JUnitPenetration())
        strategies.add(JUnitRunnerPenetration())
        strategies.add(JUnit5Penetration())
        strategies.add(JMeterPenetration())
        strategies.add(TestNGPenetration())
    }
}
