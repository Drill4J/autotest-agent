package com.epam.drill.test.agent

import com.epam.drill.test.agent.actions.*

actual object ThreadStorage {

    actual fun sessionId(): String? {
        return SessionController.sessionId.value
    }

    actual fun testName(): String? {
        return ThreadStorageStub.testName()
    }

    actual fun startSession(testName: String?) = AgentConfig.run {
        if (config.sessionForEachTest) {
            SessionController.startSession(
                customSessionId = config.sessionId,
                testName = testName
            )
        }
    }

    actual fun stopSession() = SessionController.run {
        if (AgentConfig.config.sessionForEachTest) {
            stopSession(sessionIds = sessionId.value)
        }
    }
}
