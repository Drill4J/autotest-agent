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

actual object ThreadStorage {

    actual fun memorizeTestNameNative(testName: String?) {
        SessionController.testName.value = testName ?: ""
    }

    actual fun sessionId(): String? {
        return SessionController.sessionId.value
    }

    actual fun proxyUrl(): String? {
        return SessionController._agentConfig.value.browserProxyAddress
    }

    actual fun startSession(testName: String?) = SessionController.run {
        if (agentConfig.sessionForEachTest) {
            startSession(
                customSessionId = agentConfig.sessionId,
                testName = testName
            )
        }
    }

    actual fun stopSession() = SessionController.run {
        if (agentConfig.sessionForEachTest) {
            stopSession(sessionIds = sessionId.value)
        }
    }
}
