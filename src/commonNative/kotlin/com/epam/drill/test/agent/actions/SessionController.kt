package com.epam.drill.test.agent.actions

import com.benasher44.uuid.*
import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.config.*
import com.epam.drill.test.agent.http.*
import kotlin.native.concurrent.*

object SessionController {
    val _agentConfig = AtomicReference(AgentRawConfig().freeze()).freeze()
    val agentConfig
        get() = _agentConfig.value
    val testName = AtomicReference("")
    val sessionId = AtomicReference("")

    private val dispatchActionPath: String
        get() = if (agentConfig.groupId.isBlank()) {
            "/api/agents/${agentConfig.agentId}/plugins/${agentConfig.pluginId}/dispatch-action"
        } else "/api/service-groups/${agentConfig.groupId}/plugins/${agentConfig.pluginId}/dispatch-action"


    fun startSession(
        customSessionId: String?,
        testType: String = "AUTO",
        isRealtime: Boolean = agentConfig.isRealtimeEnable,
        testName: String? = null,
        isGlobal: Boolean = agentConfig.isGlobal
    ) = runCatching {
        mainLogger.debug { "Attempting to start a Drill4J test session..." }
        val sessionId = customSessionId ?: uuid4().toString()
        val payload = StartSession.serializer() stringify StartSession(
            payload = StartSessionPayload(
                sessionId = sessionId,
                testType = testType,
                testName = testName,
                isRealtime = isRealtime,
                isGlobal = isGlobal
            )
        )
        this.sessionId.value.takeIf { it.isNotBlank() }?.let { stopSession(it) }
        this.sessionId.value = sessionId
        val response = dispatchAction(payload)
        mainLogger.debug { "Received response: ${response.body}" }
        mainLogger.info { "Started a test session with ID $sessionId" }
    }.onFailure { mainLogger.warn(it) { "Can't startSession '${sessionId.value}'" } }.getOrNull()

    fun stopSession(sessionIds: String? = null) = runCatching {
        mainLogger.debug { "Attempting to stop a Drill4J test session..." }
        val payload = StopSession.serializer() stringify stopAction(
            sessionIds ?: sessionId.value,
            runCatching { TestRun.serializer() parse TestListener.getData() }.getOrNull()
        )
        val response = dispatchAction(payload)
        mainLogger.debug { "Received response: ${response.body}" }
        mainLogger.info { "Stopped a test session with ID ${sessionId.value}" }
    }.onFailure { mainLogger.warn(it) { "Can't stopSession ${sessionId.value}" } }.getOrNull()

    private fun dispatchAction(payload: String): HttpResponse {
        val token = getToken()
        mainLogger.debug { "Auth token: $token" }
        mainLogger.debug {
            """Dispatch action: 
                                |path:$dispatchActionPath
                                |payload:$payload
                                |""".trimMargin()
        }
        return httpCall(
            agentConfig.adminAddress + dispatchActionPath, HttpRequest(
                "POST", mapOf(
                    "Authorization" to "Bearer $token",
                    "Content-Type" to "application/json"
                ), payload
            )
        ).apply { if (code != 200) error("Can't perform request: $this") }
    }

    private fun getToken(): String {
        val httpCall = httpCall(agentConfig.adminAddress + "/api/login", HttpRequest("POST"))
        if (httpCall.code != 200) error("Can't perform request: $httpCall")
        return httpCall.headers["authorization"] ?: error("No token received during login")
    }

}
