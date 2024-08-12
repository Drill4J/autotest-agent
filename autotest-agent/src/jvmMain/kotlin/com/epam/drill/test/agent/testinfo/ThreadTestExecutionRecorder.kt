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

import com.epam.drill.common.agent.request.RequestHolder
import com.epam.drill.plugins.test2code.api.TestDetails
import com.epam.drill.plugins.test2code.api.TestInfo
import com.epam.drill.plugins.test2code.api.TestResult
import com.epam.drill.test.agent.TEST_ID_HEADER
import com.epam.drill.test.agent.configuration.Configuration
import com.epam.drill.test.agent.configuration.ParameterDefinitions
import com.epam.drill.test.agent.session.ThreadStorage
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.CRC32

const val METHOD_PARAMS_KEY = "methodParams"
const val CLASS_PARAMS_KEY = "classParams"

class ThreadTestExecutionRecorder(
    private val requestHolder: RequestHolder,
    private val listeners: List<TestExecutionListener> = emptyList()
) : TestExecutionRecorder {
    private val logger = KotlinLogging.logger {}
    private val testExecutionData: ConcurrentHashMap<TestLaunchInfo, TestExecutionInfo> = ConcurrentHashMap()

    override fun recordTestStarting(
        engine: String,
        className: String,
        method: String,
        methodParams: String,
        classParams: String
    ) {
        val testLaunchId = generateTestLaunchId()
        val testLaunchInfo = TestLaunchInfo(
            engine = engine,
            path = className,
            testName = method,
            params = mapOf(
                METHOD_PARAMS_KEY to classParams,
                CLASS_PARAMS_KEY to methodParams,
            ),
            testLaunchId = testLaunchId
        )
        updateTestInfo(testLaunchInfo) {
            it.startedAt = System.currentTimeMillis()
        }
        addDrillHeaders(testLaunchId)
        listeners.forEach { it.onTestStarted(testLaunchInfo) }
        logger.debug { "Test: $testLaunchInfo STARTED" }
    }

    override fun recordTestFinishing(
        engine: String,
        className: String,
        method: String,
        methodParams: String,
        classParams: String,
        status: String
    ) {
        val testLaunchId = retrieveTestLaunchId()
        if (testLaunchId == null) {
            logger.warn { "Test $className::$method finished with result $status but no test launch id was found." }
            return
        }
        val testLaunchInfo = TestLaunchInfo(
            engine = engine,
            path = className,
            testName = method,
            params = mapOf(
                CLASS_PARAMS_KEY to classParams,
                METHOD_PARAMS_KEY to methodParams,
            ),
            testLaunchId = testLaunchId
        )
        val testResult = mapToTestResult(status)
        updateTestInfo(testLaunchInfo) {
            it.finishedAt = System.currentTimeMillis()
            it.result = testResult
        }
        clearDrillHeaders()
        listeners.forEach { it.onTestFinished(testLaunchInfo, testResult) }
        logger.debug { "Test: $testLaunchInfo FINISHED. Result: $status" }
    }

    override fun recordTestIgnoring(
        engine: String,
        className: String,
        method: String,
        methodParams: String,
        classParams: String
    ) {
        val testLaunchId = retrieveTestLaunchId() ?: generateTestLaunchId()
        val testLaunchInfo = TestLaunchInfo(
            engine = engine,
            path = className,
            testName = method,
            params = mapOf(
                CLASS_PARAMS_KEY to classParams,
                METHOD_PARAMS_KEY to methodParams,
            ),
            testLaunchId = testLaunchId
        )
        updateTestInfo(testLaunchInfo) {
            it.startedAt = 0L
            it.finishedAt = 0L
            it.result = TestResult.SKIPPED
        }
        clearDrillHeaders()
        listeners.forEach { it.onTestIgnored(testLaunchInfo) }
        logger.debug { "Test: $testLaunchInfo FINISHED. Result: ${TestResult.SKIPPED.name}" }
    }

    override fun reset() {
        testExecutionData.clear()
    }

    override fun getFinishedTests(): List<TestInfo> = testExecutionData
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
                details = testDetails
            )
        }.onEach {
            testExecutionData.remove(it.key)
        }.values.toList()

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

    private fun TestDetails.hash(): String = CRC32().let {
        it.update(this.toString().toByteArray())
        java.lang.Long.toHexString(it.value)
    }

    private fun addDrillHeaders(testLaunchId: String) {
        ThreadStorage.memorizeTestName(testLaunchId)
    }

    private fun clearDrillHeaders() {
        ThreadStorage.remove()
    }

    private fun mapToTestResult(value: String): TestResult {
        if (value == "SUCCESSFUL") return TestResult.PASSED
        return TestResult.valueOf(value)
    }

    private fun retrieveTestLaunchId(): String? = requestHolder.retrieve()?.headers?.get(TEST_ID_HEADER)
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