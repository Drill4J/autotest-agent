package com.epam.drill.test.agent.instrumentation

actual object StrategyManager {
    actual fun initialize(rawFrameworkPlugins: String, isManuallyControlled: Boolean) {
        StrategyManagerStub.initialize(rawFrameworkPlugins, isManuallyControlled)
    }
}