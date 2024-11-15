package com.epam.drill.agent.test.instrument.strategy.testing.testng

import mu.KotlinLogging

object TestNGv7PrioritizingStrategy: TestNGPrioritizingStrategy() {
    override val logger = KotlinLogging.logger {}
    override val versionRegex: Regex = "testng-7\\.[0-9]+(\\.[0-9]+)*".toRegex()

    override val id: String
        get() = "testNGv7Prioritizing"

    override fun getMethodParametersExpression(): String {
        return "getParameterTypes()"
    }
}