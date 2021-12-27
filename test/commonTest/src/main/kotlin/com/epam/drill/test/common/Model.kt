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
package com.epam.drill.test.common

import com.epam.drill.plugins.test2code.api.*
import kotlinx.serialization.*

@Serializable
data class ServerDate(
    val startedSessions: List<String>,
    val finishedSessions: List<String>,
    val tests: Map<String, List<TestInfo>>,
)

data class TestData(
    val hash: String,
    val testResult: TestResult,
)
