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
package com.epam.drill.test.agent.testinfo

import com.epam.drill.plugins.test2code.api.TestInfo

interface TestExecutionRecorder {
    fun recordTestStarting(
        engine: String,
        className: String,
        method: String,
        methodParams: String = "()",
        classParams: String = "",
    )

    fun recordTestFinishing(
        engine: String,
        className: String,
        method: String,
        methodParams: String = "()",
        classParams: String = "",
        status: String
    )

    fun recordTestIgnoring(
        engine: String,
        className: String,
        method: String,
        methodParams: String = "()",
        classParams: String = ""
    )
    
    fun getFinishedTests(): List<TestInfo>
    
    fun reset()
}