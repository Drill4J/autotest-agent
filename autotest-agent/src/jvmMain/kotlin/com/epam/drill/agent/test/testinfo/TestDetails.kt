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

import kotlinx.serialization.Serializable

@Serializable
data class TestDetails @JvmOverloads constructor(
    val engine: String = "",
    val path: String = "",
    val testName: String = "",
    val testParams: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
) : Comparable<TestDetails> {

    val signature: String
        get() = "$engine:$path.$testName(${testParams.joinToString()})"

    override fun compareTo(other: TestDetails): Int {
        return signature.compareTo(other.signature)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is TestDetails) compareTo(other) == 0 else false
    }

    override fun hashCode(): Int {
        return signature.hashCode()
    }

    override fun toString(): String {
        return signature
    }
}