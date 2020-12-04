package com.epam.drill.test.agent.instrumentation

expect object StrategyManager {
    fun initialize(rawFrameworkPlugins: String, isManuallyControlled: Boolean)
}