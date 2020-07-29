package com.epam.drill.test.agent

import com.epam.drill.kni.*

@Kni
actual object ThreadStorage {
    val storage = TTL()

    @Suppress("unused")
    fun memorizeTestName(testName: String?) {
        storage.set(testName)
        memorizeTestNameNative(testName)
    }

    actual external fun memorizeTestNameNative(testName: String?)

    actual external fun sessionId(): String?

    actual external fun proxyUrl(): String?
}