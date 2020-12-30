package com.epam.drill.test.agent

expect object ThreadStorage {
    fun memorizeTestNameNative(testName: String?)

    fun sessionId(): String?

    fun proxyUrl(): String?
    fun startSession(testName: String?)
    fun stopSession()
}
