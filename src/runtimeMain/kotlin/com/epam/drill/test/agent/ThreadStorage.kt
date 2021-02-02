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
    }

    actual external fun sessionId(): String?

    actual fun testName(): String? = storage.get()

    actual external fun startSession(testName: String?)

    actual external fun stopSession()
}
