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
import com.epam.drill.logger.*
import com.epam.drill.test.agent.actions.*
import com.epam.drill.test.agent.config.*
import com.epam.drill.test.agent.instrumentation.http.selenium.*
import com.epam.drill.test.agent.util.*
import kotlinx.atomicfu.*
import kotlinx.collections.immutable.*

@Kni
actual object TestListener {

    private val logger = Logging.logger(TestListener::class.java.simpleName)

    private val _testInfo = atomic(persistentHashMapOf<String, PersistentMap<String, Any>>())

    private fun addTestInfo(
        testId: String,
        vararg vals: Pair<String, Any>,
    ) = _testInfo.update { testProperties ->
        val currentInfo = testProperties[testId] ?: persistentHashMapOf()
        testProperties.put(testId, currentInfo + vals)
    }

    fun testStarted(test: String?) {
        test?.let {
            if (it !in _testInfo.value) {
                logger.info { "Test: $it STARTED" }
                addTestInfo(
                    test,
                    TestInfo::name.name to test,
                    TestInfo::startedAt.name to System.currentTimeMillis()
                )
                DevToolsClientThreadStorage.addHeaders(
                    mapOf(TEST_NAME_HEADER to it.urlEncode(), SESSION_ID_HEADER to (ThreadStorage.sessionId() ?: ""))
                )
                ThreadStorage.startSession(it)
                ThreadStorage.memorizeTestName(it)
                WebDriverThreadStorage.addCookies()
            } else if (isFinalizeTestState(it)) {
                logger.trace { "Test: $it was repeated. Change status to UNKNOWN" }
                addTestInfo(
                    it,
                    TestInfo::result.name to TestResult.UNKNOWN,
                    TestInfo::startedAt.name to System.currentTimeMillis()
                )
                ThreadStorage.memorizeTestName(it)
            }
        }
    }

    fun testFinished(test: String?, status: String) {
        logger.trace { "Test: $test is finishing with status $status..." }
        if (isNotFinalizeTestState(test)) {
            addTestResult(test, status)
        }
    }

    private fun addTestResult(test: String?, status: String) {
        test?.takeIf { it in _testInfo.value }?.let {
            addTestInfo(
                test,
                TestInfo::finishedAt.name to System.currentTimeMillis(),
                TestInfo::result.name to TestResult.getByMapping(status)
            )
            logger.info { "Test: $test FINISHED. Result:$status" }
        }
        ThreadStorage.stopSession()
        DevToolsClientThreadStorage.resetHeaders()
        ThreadStorage.clear()
    }


    private fun isFinalizeTestState(test: String?): Boolean = !isNotFinalizeTestState(test)

    private fun isNotFinalizeTestState(test: String?): Boolean = _testInfo.value[test]?.let { testProperties ->
        testProperties[TestInfo::result.name]?.let { result ->
            result as TestResult == TestResult.UNKNOWN
        }
    } ?: true


    fun testIgnored(test: String?) {
        test?.let {
            addTestInfo(
                test,
                TestInfo::name.name to test,
                TestInfo::startedAt.name to 0L,
                TestInfo::finishedAt.name to 0L,
                TestInfo::result.name to TestResult.SKIPPED
            )
            logger.trace { "Test: $test FINISHED. Result:${TestResult.SKIPPED.name}" }
        }
    }

    actual fun getData(): String {
        val finished = runCatching {
            _testInfo.value.filterKeys { test -> isFinalizeTestState(test) }.values.map { properties ->
                TestInfo.serializer().deserialize(PropertyDecoder(properties))
            }
        }.getOrElse {
            logger.error(it) { "Can't get tests list. Reason:" }
            emptyList()
        }
        _testInfo.update { tests -> tests - finished.map { it.name } }

        return TestRun.serializer() stringify TestRun(
            startedAt = finished.filter { it.startedAt != 0L }.minByOrNull { it.startedAt }?.startedAt ?: 0,
            finishedAt = finished.maxByOrNull { it.finishedAt }?.finishedAt ?: 0,
            tests = finished
        )
    }

    actual fun reset() {
        _testInfo.update { it.clear() }
    }
}
