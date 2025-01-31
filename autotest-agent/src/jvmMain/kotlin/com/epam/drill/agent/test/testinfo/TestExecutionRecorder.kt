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
package com.epam.drill.agent.test.testinfo

interface TestExecutionRecorder {
    fun recordTestStarting(
        testMethod: TestMethodInfo
    )

    fun recordTestFinishing(
        testMethod: TestMethodInfo,
        status: String
    )

    fun recordTestIgnoring(
        testMethod: TestMethodInfo,
        isSmartSkip: Boolean = false
    )

    fun getFinishedTests(): List<TestLaunchPayload>

    fun reset()
}

class TestMethodInfo(
    val engine: String,
    val className: String,
    val method: String,
    val methodParams: String,
    val classParams: String
)