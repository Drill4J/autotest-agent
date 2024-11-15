package com.epam.drill.agent.test.instrument.strategy.testing.testng

import mu.KotlinLogging

object TestNGv6PrioritizingStrategy: TestNGPrioritizingStrategy() {
    override val logger = KotlinLogging.logger {}
    override val versionRegex: Regex = "testng-6\\.[0-9]+(\\.[0-9]+)*".toRegex()

    override val id: String
        get() = "testNGv6Prioritizing"

    override fun getMethodParametersExpression(): String {
        return "getConstructorOrMethod().getParameterTypes()"
    }
}