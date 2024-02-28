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
package com.epam.drill.test.agent.session

import java.net.*
import com.epam.drill.test.agent.configuration.Configuration
import com.epam.drill.test.agent.configuration.ParameterDefinitions
import com.epam.drill.test.agent.serialization.json
import com.epam.drill.test.agent.session.*

object ThreadStorage {
    val storage = InheritableThreadLocal<String>()

    @Suppress("unused")
    fun memorizeTestName(testName: String?) {
        val value = testName?.let { URLEncoder.encode(it, Charsets.UTF_8.name()) }
        storage.set(value)
        SessionController.testHash = value ?: ""
    }

    fun clear() {
        storage.set(null)
    }

    fun sessionId(): String {
        return SessionController.sessionId
    }

    fun startSession(testName: String?) {
        if (Configuration.parameters[ParameterDefinitions.SESSION_FOR_EACH_TEST]) {
            SessionController.startSession(
                customSessionId = Configuration.parameters[ParameterDefinitions.SESSION_ID],
                testName = testName
            )
        }
    }

    fun stopSession() = SessionController.run {
        if (Configuration.parameters[ParameterDefinitions.SESSION_FOR_EACH_TEST]) {
            stopSession(sessionIds = sessionId)
        }
    }

    fun sendSessionData(preciseCoverage: String, scriptParsed: String, testId: String) {
        val data = SessionData(preciseCoverage, scriptParsed, testId)
        SessionController.sendSessionData(json.encodeToString(SessionData.serializer(), data))
    }

}
