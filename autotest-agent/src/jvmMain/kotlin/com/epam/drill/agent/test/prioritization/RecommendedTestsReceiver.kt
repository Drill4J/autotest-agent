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
package com.epam.drill.agent.test.prioritization

import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageReceiver
import com.epam.drill.agent.test.configuration.Configuration
import com.epam.drill.agent.test.configuration.ParameterDefinitions
import com.epam.drill.agent.test.testinfo.*
import com.epam.drill.agent.test.transport.TestAgentMessageReceiver
import com.epam.drill.agent.test2code.api.TestDetails
import kotlinx.serialization.Serializable
import mu.KotlinLogging

interface RecommendedTestsReceiver {
    fun getTestsToSkip(): List<TestDetails>
    fun sendSkippedTest(test: TestDetails)
}

class RecommendedTestsReceiverImpl(
    private val agentMessageReceiver: AgentMessageReceiver = TestAgentMessageReceiver,
    private val testExecutionRecorder: TestExecutionRecorder = TestController
) : RecommendedTestsReceiver {
    private val logger = KotlinLogging.logger {}

    override fun getTestsToSkip(): List<TestDetails> {
        if (!Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_ENABLED])
            return emptyList()
        val groupId = Configuration.parameters[ParameterDefinitions.GROUP_ID]
        val testTaskId = Configuration.parameters[ParameterDefinitions.TEST_TASK_ID]
        val targetAppId = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_TARGET_APP_ID]
        val targetBuildVersion = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_TARGET_BUILD_VERSION]
            .takeIf { it.isNotEmpty() }
        val targetCommitSha = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_TARGET_COMMIT_SHA]
            .takeIf { it.isNotEmpty() }
        val baselineCommitSha = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_BASELINE_COMMIT_SHA]
            .takeIf { it.isNotEmpty() }
        val baselineBuildVersion =
            Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_BASELINE_BUILD_VERSION]
                .takeIf { it.isNotEmpty() }
        val coveragePeriodDays =
            Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_COVERAGE_PERIOD_DAYS].toInt()
                .takeIf { it > 0 }

        val parameters: String = buildString {
            append("?groupId=$groupId")
            append("&testTaskId=$testTaskId")
            append("&targetAppId=$targetAppId")
            targetBuildVersion?.let { append("&targetBuildVersion=$it") }
            targetCommitSha?.let { append("&targetCommitSha=$it") }
            baselineCommitSha?.let { append("&baselineCommitSha=$it") }
            baselineBuildVersion?.let { append("&baselineBuildVersion=$it") }
            coveragePeriodDays?.let { append("&coveragePeriodDays=$it") }
        }
        logger.debug { "Retrieving information about recommended tests, testTaskId: $testTaskId" }
        return runCatching {
            agentMessageReceiver.receive(
                AgentMessageDestination(
                    "GET",
                    "/tests-to-skip$parameters",
                ),
                RecommendedTestsResponse::class
            ).data
        }.onFailure {
            logger.warn { "Unable to retrieve information about recommended tests. Error message: $it" }
        }.getOrElse {
            emptyList()
        }
    }

    override fun sendSkippedTest(test: TestDetails) {
        testExecutionRecorder.recordTestIgnoring(
            TestMethodInfo(
                engine = test.engine,
                className = test.path,
                method = test.testName,
                methodParams = test.testParams.joinToString(separator = ", ", prefix = "(", postfix = ")"),
                classParams = "",
            ), isSmartSkip = true
        )
    }
}

@Serializable
class RecommendedTestsResponse(
    val data: List<TestDetails>
)