package com.epam.drill.test.agent.instrumentation.http

import com.epam.drill.logger.Logging

object Log {
    private val logger = Logging.logger("headers injector")
    fun injectHeaderLog(header: String?, session: String?) {
        if (header != null && session != null)
            logger.debug { "Adding headers: $header to $session" }
    }
}