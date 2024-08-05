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
package com.epam.drill.test.agent

import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.agent.configuration.*
import com.epam.drill.test.agent.instrument.strategy.selenium.*
import com.epam.drill.test.agent.serialization.*
import com.epam.drill.test.agent.session.*
import kotlinx.atomicfu.*
import kotlinx.collections.immutable.*
import kotlinx.serialization.builtins.*
import java.util.zip.CRC32
import mu.KotlinLogging
import java.util.UUID

object TestListener {

    const val methodParamsKey = "methodParams"
    const val classParamsKey = "classParams"

    private val logger = KotlinLogging.logger {}

    private val _testInfo = atomic(persistentHashMapOf<TestDetails, PersistentMap<String, Any>>())

    private fun addTestInfo(
        testId: TestDetails,
        vararg vals: Pair<String, Any>,
    ) = _testInfo.update { testProperties ->
        val currentInfo = testProperties[testId] ?: persistentHashMapOf()
        testProperties.put(testId, currentInfo + vals)
    }

    //TODO EPMDJ-10251 add browser name for ui tests
    @JvmOverloads
    fun testStarted(
        engine: String,
        className: String?,
        method: String?,
        methodParams: String = "()",
        classParams: String = "",
    ) {
        if (className != null && method != null) {
            val test = TestDetails(
                engine = engine,
                path = className,
                testName = method,
                params = mapOf(
                    classParamsKey to classParams,
                    methodParamsKey to methodParams,
                )
            )
            val testDefinitionId = test.hash()
            val testLaunchId = UUID.randomUUID().toString()

            if (test !in _testInfo.value) {
                logger.debug { "Test: $test STARTED" }
                addTestInfo(
                    test,
                TestInfo::groupId.name to Configuration.parameters[ParameterDefinitions.GROUP_ID],
                    TestInfo::id.name to testLaunchId,
                    TestInfo::testDefinitionId.name to testDefinitionId,
                    TestInfo::details.name to test,
                    TestInfo::startedAt.name to System.currentTimeMillis()
                )
                addDrillHeaders(testLaunchId)
            } else if (isFinalizeTestState(test)) {
                restartTest(testLaunchId, test)
                addDrillHeaders(testLaunchId)
            }
        }
    }

    // TODO do we really need that logic?
    private fun restartTest(testHash: String, test: TestDetails) {
        val prevDuration = _testInfo.value[test]?.let { testProperties ->
            val startedAt = testProperties[TestInfo::startedAt.name] as Long
            val finishedAt = testProperties[TestInfo::finishedAt.name] as Long
            finishedAt - startedAt
        } ?: 0L
        logger.trace { "Test: $test was repeated, prev duration $prevDuration. Change status to UNKNOWN" }
        addTestInfo(
            test,
            TestInfo::groupId.name to Configuration.parameters[ParameterDefinitions.GROUP_ID],
            TestInfo::id.name to testHash,
            TestInfo::details.name to test,
            TestInfo::result.name to TestResult.UNKNOWN,
            TestInfo::startedAt.name to System.currentTimeMillis() - prevDuration
        )
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
            val test = TestDetails(
                engine = engine,
                path = className,
                testName = method,
                params = mapOf(
                    classParamsKey to classParams,
                    methodParamsKey to methodParams,
                ),
            )
            logger.trace { "Test: $test is finishing with status $status..." }
            if (isNotFinalizeTestState(test)) {
                addTestResult(test, status)
            }
        }
    }

    private fun addTestResult(test: TestDetails?, status: String) {
        test?.takeIf { it in _testInfo.value }?.let {
            addTestInfo(
                test,
                TestInfo::groupId.name to Configuration.parameters[ParameterDefinitions.GROUP_ID],
                TestInfo::finishedAt.name to System.currentTimeMillis(),
                TestInfo::result.name to getByMapping(status)
            )
            logger.debug { "Test: $test FINISHED. Result:$status" }
            clearDrillHeaders(it)
            if (Configuration.parameters[ParameterDefinitions.WITH_JS_COVERAGE]) sendSessionData(test.hash())
        }
    }

    private fun isFinalizeTestState(test: TestDetails?): Boolean = !isNotFinalizeTestState(test)

    private fun isNotFinalizeTestState(test: TestDetails?): Boolean = _testInfo.value[test]?.let { testProperties ->
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
            val test = TestDetails(
                engine = engine,
                path = className,
                testName = method,
                params = mapOf(
                    classParamsKey to classParams,
                    methodParamsKey to methodParams,
                )
            )
            clearDrillHeaders(test)
            addTestInfo(
                test,
                TestInfo::id.name to test.hash(),
                TestInfo::details.name to test,
                TestInfo::startedAt.name to 0L,
                TestInfo::finishedAt.name to 0L,
                TestInfo::result.name to TestResult.SKIPPED
            )
            logger.trace { "Test: $test FINISHED. Result:${TestResult.SKIPPED.name}" }
        }
    }

    private fun addDrillHeaders(testHash: String) {
        ThreadStorage.startSession()
        ThreadStorage.memorizeTestName(testHash)
        DevToolStorage.get()?.startIntercept()
        WebDriverThreadStorage.addCookies()
    }

    /**
     * Removing headers only for started tests
     */
    private fun clearDrillHeaders(test: TestDetails) {
        if (test in _testInfo.value) {
            ThreadStorage.stopSession()
            DevToolStorage.get()?.stopIntercept()
            ThreadStorage.clear()
        }
    }

    fun getData(): String {
        val finished = runCatching {
            _testInfo.value.filterKeys { test -> isFinalizeTestState(test) }.values.map { properties ->
                TestInfo.serializer().deserialize(PropertyDecoder(properties))
            }
        }.getOrElse {
            logger.error(it) { "Can't get tests list. Reason:" }
            emptyList()
        }
        _testInfo.update { tests -> tests - finished.map { it.details } }

        return json.encodeToString(ListSerializer(TestInfo.serializer()), finished)
    }

    fun reset() {
        _testInfo.update { it.clear() }
    }

    private fun getByMapping(value: String): TestResult {
        if (value == "SUCCESSFUL") return TestResult.PASSED
        return TestResult.valueOf(value)
    }

    private fun sendSessionData(testId: String) = DevToolStorage.get()?.run {
        val coverage = takePreciseCoverage()
        if (coverage.isBlank()) {
            logger.trace { "coverage is blank" }
            return null
        }
        val scripts = scriptParsed()
        if (scripts.isBlank()) {
            logger.trace { "script parsed is blank" }
            return null
        }
        logger.debug { "ThreadStorage.sendSessionData" }
        ThreadStorage.sendSessionData(coverage, scripts, testId)
    }

    private fun TestDetails.hash(): String = CRC32().let {
        it.update(this.toString().toByteArray())
        java.lang.Long.toHexString(it.value)
    }

}
