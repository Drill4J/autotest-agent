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
import com.epam.drill.agent.test.transport.TestAgentMessageReceiver
import com.epam.drill.agent.test2code.api.TestDetails
import kotlinx.serialization.Serializable

interface RecommendedTestsReceiver {
    fun getTestsToSkip(groupId: String, testTaskId: String, filterCoverageDays: Int?): List<TestDetails>
}

class RecommendedTestsReceiverImpl(
    private val agentMessageReceiver: AgentMessageReceiver = TestAgentMessageReceiver
): RecommendedTestsReceiver {
    override fun getTestsToSkip(groupId: String, testTaskId: String, filterCoverageDays: Int?): List<TestDetails> {
        val parameters = if (filterCoverageDays != null) "?filterCoverageDays=$filterCoverageDays" else ""
        return agentMessageReceiver.receive(
            AgentMessageDestination(
                "GET",
                "/tests-to-skip/$groupId/$testTaskId$parameters",
            ),
            RecommendedTestsResponse::class
        ).tests
    }
}

@Serializable
class RecommendedTestsResponse(
    val tests: List<TestDetails>
)