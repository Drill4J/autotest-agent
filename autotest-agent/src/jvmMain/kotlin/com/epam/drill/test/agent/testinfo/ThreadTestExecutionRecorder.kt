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

import com.epam.drill.agent.request.DrillRequestHolder
import com.epam.drill.agent.common.request.DrillRequest
import com.epam.drill.agent.common.request.RequestHolder
import com.epam.drill.plugins.test2code.api.TestDetails
import com.epam.drill.plugins.test2code.api.TestInfo
import com.epam.drill.plugins.test2code.api.TestResult
import com.epam.drill.test.agent.TEST_ID_HEADER
import com.epam.drill.test.agent.configuration.Configuration
import com.epam.drill.test.agent.configuration.ParameterDefinitions
import com.epam.drill.test.agent.session.SessionController
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
        testMethod: TestMethodInfo
    ) {
        val testLaunchId = generateTestLaunchId()
        val testLaunchInfo = mapToLaunchInfo(testMethod, testLaunchId)
        updateTestInfo(testLaunchInfo) {
            it.startedAt = System.currentTimeMillis()
        }
        addDrillHeaders(testLaunchId)
        listeners.forEach { it.onTestStarted(testLaunchInfo) }
        logger.debug { "Test: $testLaunchInfo STARTED" }
    }

    override fun recordTestFinishing(
        testMethod: TestMethodInfo,
        status: String
    ) {
        val testLaunchId = getTestLaunchId()
        if (testLaunchId == null) {
            logger.warn { "Test ${testMethod.className}::${testMethod.method} finished with result $status but no test launch id was found." }
            return
        }
        val testLaunchInfo = mapToLaunchInfo(testMethod, testLaunchId)
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
        testMethod: TestMethodInfo
    ) {
        val testLaunchId = getTestLaunchId() ?: generateTestLaunchId()
        val testLaunchInfo = mapToLaunchInfo(testMethod, testLaunchId)
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
        DrillRequestHolder.store(
            DrillRequest(
                drillSessionId = SessionController.getSessionId(),
                headers = mapOf(TEST_ID_HEADER to (testLaunchId))
            )
        )
    }

    private fun clearDrillHeaders() {
        DrillRequestHolder.remove()
    }

    private fun mapToTestResult(value: String): TestResult {
        if (value == "SUCCESSFUL") return TestResult.PASSED
        return TestResult.valueOf(value)
    }

    private fun getTestLaunchId(): String? = requestHolder.retrieve()?.headers?.get(TEST_ID_HEADER)

    private fun mapToLaunchInfo(
        testMethod: TestMethodInfo,
        testLaunchId: String
    ) = TestLaunchInfo(
        engine = testMethod.engine,
        path = testMethod.className,
        testName = testMethod.method,
        params = mapOf(
            CLASS_PARAMS_KEY to testMethod.classParams,
            METHOD_PARAMS_KEY to testMethod.methodParams,
        ),
        testLaunchId = testLaunchId
    )
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