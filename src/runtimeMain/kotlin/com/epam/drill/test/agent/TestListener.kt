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
import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.agent.config.*
import com.epam.drill.test.agent.instrumentation.http.selenium.*
import com.epam.drill.test.agent.util.*
import kotlinx.atomicfu.*
import kotlinx.collections.immutable.*

@Kni
actual object TestListener {

    private val logger = Logging.logger(TestListener::class.java.simpleName)

    private val _testInfo = atomic(persistentHashMapOf<TestName, PersistentMap<String, Any>>())

    private fun addTestInfo(
        testId: TestName,
        vararg vals: Pair<String, Any>,
    ) = _testInfo.update { testProperties ->
        val currentInfo = testProperties[testId] ?: persistentHashMapOf()
        testProperties.put(testId, currentInfo + vals)
    }

    @JvmOverloads
    fun testStarted(
        engine: String,
        className: String?,
        method: String?,
        methodParams: String = "()",
        classParams: String = "",
    ) {
        if (className != null && method != null) {
            val test = TestName(
                engine = engine,
                className = className,
                method = method,
                classParams = classParams,
                methodParams = methodParams,
            )
            if (test !in _testInfo.value) {
                logger.info { "Test: $test STARTED" }
                addTestInfo(
                    test,
                    TestInfo::name.name to test.fullName,
                    TestInfo::testName.name to test,
                    TestInfo::startedAt.name to System.currentTimeMillis()
                )
                DevToolsClientThreadStorage.addHeaders(
                    mapOf(TEST_NAME_HEADER to test.fullName.urlEncode(),
                        SESSION_ID_HEADER to (ThreadStorage.sessionId() ?: ""))
                )
                ThreadStorage.startSession(test.fullName)
                ThreadStorage.memorizeTestName(test.fullName)
                WebDriverThreadStorage.addCookies()
            } else if (isFinalizeTestState(test)) {
                logger.trace { "Test: $test was repeated. Change status to UNKNOWN" }
                addTestInfo(
                    test,
                    TestInfo::result.name to TestResult.UNKNOWN,
                    TestInfo::startedAt.name to System.currentTimeMillis()
                )
                ThreadStorage.memorizeTestName(test.fullName)
            }
        }
    }

    @JvmOverloads
    fun testFinished(
        engine: String,
        className: String?,
        method: String?,
        status: String,
        methodParams: String = "()",
        classParams: String = "",
    ) {
        if (className != null && method != null) {
            val testName = TestName(
                engine = engine,
                className = className,
                method = method,
                classParams = classParams,
                methodParams = methodParams,
            )
            logger.trace { "Test: $testName is finishing with status $status..." }
            if (isNotFinalizeTestState(testName)) {
                addTestResult(testName, status)
            }
        }
    }

    private fun addTestResult(test: TestName?, status: String) {
        test?.takeIf { it in _testInfo.value }?.let {
            addTestInfo(
                test,
                TestInfo::finishedAt.name to System.currentTimeMillis(),
                TestInfo::result.name to getByMapping(status)
            )
            logger.info { "Test: $test FINISHED. Result:$status" }
        }
        ThreadStorage.stopSession()
        DevToolsClientThreadStorage.resetHeaders()
        ThreadStorage.clear()
    }


    private fun isFinalizeTestState(test: TestName?): Boolean = !isNotFinalizeTestState(test)

    private fun isNotFinalizeTestState(test: TestName?): Boolean = _testInfo.value[test]?.let { testProperties ->
        testProperties[TestInfo::result.name]?.let { result ->
            result as TestResult == TestResult.UNKNOWN
        }
    } ?: true

    @JvmOverloads
    fun testIgnored(
        engine: String,
        className: String?,
        method: String?,
        methodParams: String = "()",
        classParams: String = "",
    ) {
        if (className != null && method != null) {
            val test = TestName(
                engine = engine,
                className = className,
                method = method,
                classParams = classParams,
                methodParams = methodParams,
            )
            addTestInfo(
                test,
                TestInfo::name.name to test.fullName,
                TestInfo::testName.name to test,
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
        _testInfo.update { tests -> tests - finished.mapNotNull { it.testName } }

        return TestRun.serializer() stringify TestRun(
            startedAt = finished.filter { it.startedAt != 0L }.minByOrNull { it.startedAt }?.startedAt ?: 0,
            finishedAt = finished.maxByOrNull { it.finishedAt }?.finishedAt ?: 0,
            tests = finished
        )
    }

    actual fun reset() {
        _testInfo.update { it.clear() }
    }

    private fun getByMapping(value: String): TestResult {
        if (value == "SUCCESSFUL") return TestResult.PASSED
        return TestResult.valueOf(value)
    }
}

