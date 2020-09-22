package com.epam.drill

import com.epam.drill.kni.Kni

@Kni
object SessionProvider {

    external fun startSession(
        sessionId: String,
        testType: String = "AUTO",
        isRealtime: Boolean = false,
        testName: String? = null,
        isGlobal: Boolean = false
    )

    external fun stopSession(sessionId: String? = null)

    external fun setTestName(testName: String?)
}



