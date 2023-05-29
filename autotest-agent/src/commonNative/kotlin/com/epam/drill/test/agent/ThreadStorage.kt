/**
 * Copyright 2020 EPAM Systems
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
package com.epam.drill.test.agent

import com.epam.drill.test.agent.actions.*
import com.epam.drill.test.agent.config.*
import com.epam.drill.test.agent.js.*

actual object ThreadStorage {

    actual fun memorizeTestNameNative(testName: String?) {
        SessionController.testHash.value = testName ?: ""
    }

    actual fun sessionId(): String? {
        return SessionController.sessionId.value
    }

    actual fun startSession(testName: String?) {
        if (AgentConfig.config.sessionForEachTest) {
            SessionController.startSession(
                customSessionId = AgentConfig.config.sessionId,
                testName = testName
            )
        }
    }

    actual fun stopSession() = SessionController.run {
        if (AgentConfig.config.sessionForEachTest) {
            stopSession(sessionIds = sessionId.value)
        }
    }

    actual fun sendSessionData(preciseCoverage: String, scriptParsed: String, testId: String) {
        val data = JsSessionData(preciseCoverage, scriptParsed, testId)
        SessionController.sendSessionData(JsSessionData.serializer() stringify data)
    }
}
