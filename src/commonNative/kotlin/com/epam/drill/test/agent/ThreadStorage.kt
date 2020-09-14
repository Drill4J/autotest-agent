package com.epam.drill.test.agent

import com.epam.drill.test.agent.actions.*

actual object ThreadStorage {
    actual fun memorizeTestNameNative(testName: String?) {
        SessionController.testName.value = testName ?: ""
    }

    actual fun sessionId(): String? {
        return SessionController.sessionId.value
    }

    actual fun proxyUrl(): String? {
        return SessionController._agentConfig.value.browserProxyAddress
    }
}