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

import com.epam.drill.kni.*
import com.epam.drill.test.agent.config.*
import com.epam.drill.test.agent.js.*
import com.epam.drill.test.agent.util.*

@Kni
actual object ThreadStorage {
    val storage = TTL()

    /**
     * Store the test name in the current thread
     * @param testName the name of the test
     * @features Running tests
     */
    @Suppress("unused")
    fun memorizeTestName(testName: String?) {
        val value = testName?.urlEncode()
        storage.set(value)
        memorizeTestNameNative(value)
    }

    /**
     * Clear the test name from the current thread
     * @features Running tests
     */
    fun clear() {
        storage.set(null)
    }

    actual external fun memorizeTestNameNative(testName: String?)

    actual external fun sessionId(): String?

    /**
     * Start a new test session
     * @param testName the first test of the test session
     * @features Session starting
     */
    actual external fun startSession(testName: String?)

    /**
     * Stop the test session
     * @features Session finishing
     */
    actual external fun stopSession()

    /**
     * Send results of the tests
     * @features Running tests
     */
    actual external fun sendSessionData(preciseCoverage: String, scriptParsed: String, testId: String)
}
