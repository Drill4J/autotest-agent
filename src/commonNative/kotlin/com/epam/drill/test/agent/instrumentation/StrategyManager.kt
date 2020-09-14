package com.epam.drill.test.agent.instrumentation

actual object StrategyManager {
    actual fun initialize(rawFrameworkPlugins: String) {
        StrategyManagerStub.initialize(rawFrameworkPlugins)
    }
}