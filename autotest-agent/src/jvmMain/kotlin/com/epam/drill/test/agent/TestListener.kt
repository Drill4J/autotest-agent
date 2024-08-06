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
import kotlinx.serialization.builtins.*
import java.util.zip.CRC32
import mu.KotlinLogging
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object TestListener {

    const val methodParamsKey = "methodParams"
    const val classParamsKey = "classParams"

    private val logger = KotlinLogging.logger {}

    private val testExecutionData: ConcurrentHashMap<TestLaunchInfo, TestExecutionInfo> = ConcurrentHashMap()


    //TODO EPMDJ-10251 add browser name for ui tests
    @JvmOverloads
    fun testStarted(
        engine: String,
        className: String?,
        method: String?,
        methodParams: String = "()",
        classParams: String = "",
    ) {
        if (className == null || method == null)
            return

        val testLaunchId = generateTestLaunchId()
        val testLaunchInfo = TestLaunchInfo(
            engine = engine,
            path = className,
            testName = method,
            params = mapOf(
                classParamsKey to classParams,
                methodParamsKey to methodParams,
            ),
            testLaunchId = testLaunchId
        )
        updateTestInfo(testLaunchInfo) {
            it.startedAt = System.currentTimeMillis()
        }
        addDrillHeaders(testLaunchId)
        logger.debug { "Test: $testLaunchInfo STARTED" }
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
        if (className == null || method == null)
            return

        val testLaunchId = ThreadStorage.retrieveTestLaunchId() ?: return
        val testLaunchInfo = TestLaunchInfo(
            engine = engine,
            path = className,
            testName = method,
            params = mapOf(
                classParamsKey to classParams,
                methodParamsKey to methodParams,
            ),
            testLaunchId = testLaunchId
        )
        updateTestInfo(testLaunchInfo) {
            it.finishedAt = System.currentTimeMillis()
            it.result = getByMapping(status)
        }
        if (Configuration.parameters[ParameterDefinitions.WITH_JS_COVERAGE])
            sendSessionData(testLaunchId)
        clearDrillHeaders()
        logger.debug { "Test: $testLaunchInfo FINISHED. Result: $status" }
    }

    @JvmOverloads
    fun testIgnored(
        engine: String,
        className: String?,
        method: String?,
        methodParams: String = "()",
        classParams: String = "",
    ) {
        if (className == null || method == null)
            return

        val testLaunchId = ThreadStorage.retrieveTestLaunchId() ?: generateTestLaunchId()
        val testLaunchInfo = TestLaunchInfo(
            engine = engine,
            path = className,
            testName = method,
            params = mapOf(
                classParamsKey to classParams,
                methodParamsKey to methodParams,
            ),
            testLaunchId = testLaunchId
        )
        updateTestInfo(testLaunchInfo) {
            it.startedAt = 0L
            it.finishedAt = 0L
            it.result = TestResult.SKIPPED
        }
        clearDrillHeaders()
        logger.debug { "Test: $testLaunchInfo FINISHED. Result: ${TestResult.SKIPPED.name}" }
    }

    private fun addDrillHeaders(testLaunchId: String) {
        ThreadStorage.memorizeTestName(testLaunchId)
        DevToolStorage.get()?.startIntercept()
        WebDriverThreadStorage.addCookies()
    }

    /**
     * Removing headers only for started tests
     */
    private fun clearDrillHeaders() {
        DevToolStorage.get()?.stopIntercept()
        ThreadStorage.clear()
    }

    fun retrieveData(): String {
        val finished = testExecutionData
            .filterValues { test -> test.result != TestResult.UNKNOWN }
            .mapValues { (launchInfo, executionInfo) ->
                val testDetails = TestDetails(
                    engine = launchInfo.engine,
                    path = launchInfo.path,
                    testName = launchInfo.testName,
                    params = launchInfo.params
                )
                TestInfo(
                    groupId = Configuration.parameters[ParameterDefinitions.GROUP_ID],
                    id = launchInfo.testLaunchId,
                    testDefinitionId = testDetails.hash(),
                    result = executionInfo.result,
                    startedAt = executionInfo.startedAt ?: 0L,
                    finishedAt = executionInfo.finishedAt ?: 0L,
                    details = testDetails,
                )
            }
        finished.keys.forEach { testExecutionData.remove(it) }
        return json.encodeToString(ListSerializer(TestInfo.serializer()), finished.values.toList())
    }

    fun reset() {
        testExecutionData.clear()
    }

    private fun getByMapping(value: String): TestResult {
        if (value == "SUCCESSFUL") return TestResult.PASSED
        return TestResult.valueOf(value)
    }

    private fun sendSessionData(testLaunchId: String) = DevToolStorage.get()?.run {
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
        ThreadStorage.sendSessionData(coverage, scripts, testLaunchId)
    }

    private fun TestDetails.hash(): String = CRC32().let {
        it.update(this.toString().toByteArray())
        java.lang.Long.toHexString(it.value)
    }

    private fun updateTestInfo(
        testLaunchInfo: TestLaunchInfo,
        updateTestExecutionInfo: (TestExecutionInfo) -> Unit,
    ) {
        testExecutionData.compute(testLaunchInfo) { _, value ->
            val testExecutionInfo = value ?: TestExecutionInfo()
            updateTestExecutionInfo(testExecutionInfo)
            testExecutionInfo
        }
    }

    private fun generateTestLaunchId() = UUID.randomUUID().toString()

}

class TestExecutionInfo(
    var result: TestResult = TestResult.UNKNOWN,
    var startedAt: Long? = null,
    var finishedAt: Long? = null,
)

data class TestLaunchInfo(
    val engine: String = "",
    val path: String = "",
    val testName: String = "",
    val params: Map<String, String> = emptyMap(),
    val testLaunchId: String,
)