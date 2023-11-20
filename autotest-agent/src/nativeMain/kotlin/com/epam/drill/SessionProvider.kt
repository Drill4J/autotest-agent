/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill

import com.epam.drill.jvmapi.callObjectVoidMethodWithString
import com.epam.drill.test.agent.TestListener
import com.epam.drill.test.agent.ThreadStorage
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
        callObjectVoidMethodWithString(ThreadStorage::class, "memorizeTestName", testName ?: "unspecified")
    }

}
