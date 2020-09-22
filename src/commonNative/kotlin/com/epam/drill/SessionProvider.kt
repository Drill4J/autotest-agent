package com.epam.drill

import com.epam.drill.test.agent.TestListener
import com.epam.drill.test.agent.ThreadStorageStub
import com.epam.drill.test.agent.actions.SessionController

object SessionProvider {

    fun startSession(
        sessionId: String,
        testType: String = "AUTO",
        isRealtime: Boolean = false,
        testName: String? = null,
        isGlobal: Boolean = false
    ) {
        SessionController.startSession(sessionId, testType, isRealtime, testName, isGlobal)
        TestListener.reset()
    }

    fun stopSession(sessionId: String? = null) {
        SessionController.stopSession(sessionId)
    }

    fun setTestName(testName: String?) {
        ThreadStorageStub.memorizeTestName(testName ?: "unspecified")
    }
}