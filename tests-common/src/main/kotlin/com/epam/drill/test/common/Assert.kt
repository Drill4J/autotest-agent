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
package com.epam.drill.test.common

import com.epam.drill.plugins.test2code.api.*
import kotlin.test.*

fun List<TestInfo>.assertTestTime() = forEach {
    when (it.result) {
        TestResult.SKIPPED -> assertTrue(it.finishedAt == it.startedAt && it.startedAt == 0L)
        else -> assertTrue(it.finishedAt >= it.startedAt && it.finishedAt > 0)
    }
}

infix fun List<TestInfo>.shouldContainsAllTests(expected: Collection<TestData>) {
    val actual = map { it.toTestData() }
    assertEquals(expected.size, actual.size)
    assertTrue(expected.containsAll(actual))
    assertEquals(expected.count { it.testResult == TestResult.SKIPPED }, count { it.result == TestResult.SKIPPED })
}
