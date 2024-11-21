package com.epam.drill.agent.test.prioritization

import com.epam.drill.agent.common.transport.AgentMessageDestination
import com.epam.drill.agent.common.transport.AgentMessageReceiver
import com.epam.drill.agent.test.configuration.Configuration
import com.epam.drill.agent.test.configuration.ParameterDefinitions
import com.epam.drill.agent.test2code.api.TestDetails
import com.epam.drill.agent.transport.JsonAgentMessageDeserializer
import com.epam.drill.agent.transport.JsonAgentMessageSerializer
import com.epam.drill.agent.transport.SimpleAgentMessageReceiver
import com.epam.drill.agent.transport.http.TypedHttpAgentMessageTransport
import kotlinx.serialization.serializer

interface RecommendedTestsReceiver {
    fun getTestsToSkip(groupId: String, testTaskId: String, filterCoverageDays: Int?): List<TestDetails>
}

class RecommendedTestsReceiverImpl(
    private val agentMessageReceiver: AgentMessageReceiver<RecommendedTestsResponse> = SimpleAgentMessageReceiver(
        TypedHttpAgentMessageTransport(
            serverAddress = Configuration.parameters[ParameterDefinitions.API_URL],
            apiKey = Configuration.parameters[ParameterDefinitions.API_KEY],
            gzipCompression = false,
            messageSerializer = JsonAgentMessageSerializer(),
            messageDeserializer = JsonAgentMessageDeserializer(RecommendedTestsResponse::class.serializer()),
        )
    )
): RecommendedTestsReceiver {
    override fun getTestsToSkip(groupId: String, testTaskId: String, filterCoverageDays: Int?): List<TestDetails> {
        val parameters = if (filterCoverageDays != null) "?filterCoverageDays=$filterCoverageDays" else ""
        return agentMessageReceiver.receive(
            AgentMessageDestination(
                "GET",
                "/recommended-tests/$groupId/$testTaskId$parameters",
            )
        ).tests
    }
}

data class RecommendedTestsResponse(
    val tests: List<TestDetails>
)