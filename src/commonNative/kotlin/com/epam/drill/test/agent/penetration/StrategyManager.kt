package com.epam.drill.test.agent.penetration

actual object StrategyManager {
    actual fun initialize(rawFrameworkPlugins: String) {
        StrategyManagerStub.initialize(rawFrameworkPlugins)
    }
}