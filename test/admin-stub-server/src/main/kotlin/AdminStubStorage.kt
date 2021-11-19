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

import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.common.*


class AdminStubStorage {

    private val startedSessions = mutableListOf<String>()
    private val finishedSessions = mutableListOf<String>()
    private val tests = mutableMapOf<String, List<TestInfo>>()

    fun addAction(rawAction: String) {
        when (val action = parseAction(rawAction)) {
            is StartNewSession -> {
                startedSessions.add(action.payload.sessionId)
            }
            is AddTests -> {
                action.payload.let {
                    val testsInfo = tests[action.payload.sessionId] ?: listOf()
                    tests[action.payload.sessionId] = testsInfo + it.tests
                }
            }
            is StopSession -> {
                startedSessions.remove(action.payload.sessionId)
                finishedSessions.add(action.payload.sessionId)
                action.payload.let {
                    val testsInfo = tests[action.payload.sessionId] ?: listOf()
                    tests[action.payload.sessionId] = testsInfo + it.tests
                }
            }
        }
    }

    fun dump() = ServerDate(startedSessions, finishedSessions, tests).encode().also {
        tests.clear()
    }
}
