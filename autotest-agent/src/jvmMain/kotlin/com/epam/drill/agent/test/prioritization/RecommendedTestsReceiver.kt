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
    fun getTestsToSkip(filterCoverageDays: Int?): List<TestDetails>
    fun sendSkippedTest(test: TestDetails)
}

class RecommendedTestsReceiverImpl(
    private val agentMessageReceiver: AgentMessageReceiver = TestAgentMessageReceiver,
    private val testExecutionRecorder: TestExecutionRecorder = TestController
) : RecommendedTestsReceiver {
    private val logger = KotlinLogging.logger {}

    override fun getTestsToSkip(filterCoverageDays: Int?): List<TestDetails> {
        val groupId = Configuration.parameters[ParameterDefinitions.GROUP_ID]
        val testTaskId = Configuration.parameters[ParameterDefinitions.TEST_TASK_ID]
        val parameters = if (filterCoverageDays != null) "?filterCoverageDays=$filterCoverageDays" else ""
        return runCatching {
            agentMessageReceiver.receive(
                AgentMessageDestination(
                    "GET",
                    "/tests-to-skip/$groupId/$testTaskId$parameters",
                ),
                RecommendedTestsResponse::class
            ).data
        }.getOrElse {
            logger.warn { "Unable to retrieve information about recommended tests. All tests will be run. Error message: $it" }
            emptyList()
        }
    }

    override fun sendSkippedTest(test: TestDetails) {
        testExecutionRecorder.recordTestIgnoring(
            TestMethodInfo(
                engine = test.engine,
                className = test.path,
                method = test.testName,
                methodParams = test.params[METHOD_PARAMS_KEY] ?: "()",
                classParams = test.params[CLASS_PARAMS_KEY] ?: "",
            ), isSmartSkip = true
        )
    }
}

@Serializable
class RecommendedTestsResponse(
    val data: List<TestDetails>
)