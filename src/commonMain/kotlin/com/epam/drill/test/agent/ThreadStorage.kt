package com.epam.drill.test.agent

expect object ThreadStorage {
    fun sessionId(): String?
    fun testName(): String?

    fun startSession(testName: String?)
    fun stopSession()
}
