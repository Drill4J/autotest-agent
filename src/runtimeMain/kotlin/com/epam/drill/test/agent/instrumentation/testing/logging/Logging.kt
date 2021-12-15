package com.epam.drill.test.agent.instrumentation.testing.logging

import com.epam.drill.logger.Logging

object Logging {
    private val logger = Logging.logger("TestStrategy")

    fun debug(message: String) {
        logger.debug { message }
    }
}
