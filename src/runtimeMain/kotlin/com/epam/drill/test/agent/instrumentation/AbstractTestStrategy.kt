@file:Suppress("LeakingThis")

package com.epam.drill.test.agent.instrumentation

abstract class AbstractTestStrategy : Strategy() {
    init {
        StrategyManager.allStrategies[id] =
            (StrategyManager.allStrategies[id] ?: mutableSetOf()).apply { add(this@AbstractTestStrategy) }
    }

    abstract val id: String
}
