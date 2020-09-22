@file:JvmName("Drill")

package com.epam.drill

class Drill {
    companion object {

        @JvmStatic
        @JvmOverloads
        fun startSession(
            sessionId: String,
            testType: String = "AUTO",
            isRealtime: Boolean = false,
            testName: String? = null,
            isGlobal: Boolean = false
        ) {
            runCatching {
                SessionProvider.startSession(
                    sessionId = sessionId,
                    testType = testType,
                    isRealtime = isRealtime,
                    testName = testName,
                    isGlobal = isGlobal
                )
            }.onFailure { println("can't start session");it.printStackTrace() }/**/
        }

        @JvmStatic
        fun stopSession(sessionId: String? = null) {
            runCatching {
                SessionProvider.stopSession(sessionId)
            }.onFailure { println("can't start session");it.printStackTrace() }/**/
        }

        @JvmStatic
        fun setTestName(testName: String?) {
            runCatching {
                SessionProvider.setTestName(testName)
            }.onFailure { println("can't start session");it.printStackTrace() }/**/
        }
    }
}