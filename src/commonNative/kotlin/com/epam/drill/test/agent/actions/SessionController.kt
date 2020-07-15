package com.epam.drill.test.agent.actions

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.config.*
import com.epam.drill.test.agent.http.*
import kotlinx.serialization.builtins.*
import kotlin.native.concurrent.*

object SessionController {
    val agentConfig = AtomicReference(AgentRawConfig().freeze()).freeze()
    val testName = AtomicReference("")
    val sessionId = AtomicReference("")

    private val dispatchActionPath: String
        get() = if (agentConfig.value.groupId.isBlank()) {
            "/api/agents/${agentConfig.value.agentId}/plugins/${agentConfig.value.pluginId}/dispatch-action"
        } else "/api/service-groups/${agentConfig.value.groupId}/plugins/${agentConfig.value.pluginId}/dispatch-action"

    fun startSession(customSessionId: String?) {
        mainLogger.debug { "Attempting to start a Drill4J test session..." }
        val payload =
            StartSession.serializer() stringify StartSession(payload = StartPayload(sessionId = customSessionId ?: ""))
        sessionId.value = customSessionId ?: ""
        val response = dispatchAction(payload)
        mainLogger.debug { "Received response: ${response.body}" }
        val startSessionResponse = if (agentConfig.value.groupId.isBlank())
            StartSessionResponse.serializer() parse response.body
        else (StartSessionResponse.serializer().list parse response.body).first()
        sessionId.value = startSessionResponse.data.payload.sessionId
        mainLogger.info { "Started a test session with ID ${sessionId.value}" }
    }

    fun stopSession() {
        mainLogger.debug { "Attempting to stop a Drill4J test session..." }
        val payload = StopSession.serializer() stringify stopAction(sessionId.value)
        val response = dispatchAction(payload)
        mainLogger.debug { "Received response: ${response.body}" }
        mainLogger.info { "Stopped a test session with ID ${sessionId.value}" }
    }

    private fun dispatchAction(payload: String): HttpResponse {
        val token = getToken()
        mainLogger.debug { "Auth token: $token" }
        return Sender.post(
            agentConfig.value.adminHost,
            agentConfig.value.adminPort,
            dispatchActionPath,
            mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            payload
        )
    }

    private fun getToken(): String = Sender.post(
        agentConfig.value.adminHost,
        agentConfig.value.adminPort,
        "/api/login"
    ).headers["Authorization"] ?: error("No token received during login")

}