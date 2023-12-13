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
@file:JvmName("Drill")
package com.epam.drill.test.agent

import com.epam.drill.test.agent.session.SessionProvider

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