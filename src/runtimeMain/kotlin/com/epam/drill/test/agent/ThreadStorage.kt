package com.epam.drill.test.agent

import com.epam.drill.kni.*
import com.epam.drill.test.agent.instrumentation.http.selenium.*
import java.net.URLEncoder

@Kni
actual object ThreadStorage {
    val storage = TTL()

    @Suppress("unused")
    fun memorizeTestName(testName: String?) {
        val value = URLEncoder.encode(testName, "UTF-8")
        storage.set(value)
        memorizeTestNameNative(value)
    }

    actual external fun memorizeTestNameNative(testName: String?)

    actual external fun sessionId(): String?

    actual external fun proxyUrl(): String?

    actual external fun startSession(testName: String?)

    actual external fun stopSession()
}
