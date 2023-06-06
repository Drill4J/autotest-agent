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
package com.epam.drill.test.agent.actions

import com.benasher44.uuid.*
import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.config.*
import com.epam.drill.test.agent.http.*
import com.epam.drill.test.agent.js.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.*
import kotlin.native.concurrent.*
import kotlin.time.*

object SessionController {
    private val agentConfig = AgentConfig.config
    val testHash = AtomicReference("undefined")
    val sessionId = AtomicReference("")
    private val token = AtomicReference("")

    private val dispatchActionPath: String
        get() = if (agentConfig.groupId.isBlank()) {
            "/api/agents/${agentConfig.agentId}/plugins/${agentConfig.pluginId}/dispatch-action"
        } else "/api/groups/${agentConfig.groupId}/plugins/${agentConfig.pluginId}/dispatch-action"

    init {
        GlobalScope.launch {
            while (true) {
                delay(Duration.seconds(3))
                runCatching {
                    val tests = runCatching {
                        ListSerializer(TestInfo.serializer()) parse TestListener.getData()
                    }.getOrNull() ?: emptyList()
                    if (tests.any()) {
                        sendTests(tests)
                    }
                }.onFailure { mainLogger.error(it) { "Can't parse tests. Reason:" } }
            }
        }
    }

    private fun sendTests(tests: List<TestInfo>) {
        val payload = Action.serializer() stringify AddTests(
            payload = AddTestsPayload(ThreadStorage.sessionId() ?: "", tests)
        )
        val result = dispatchAction(payload)
        mainLogger.trace { "Count of tests sent: ${tests.size}, received status ${result.code}" }
    }

    /**
     * Start a test session
     * @param customSessionId the predefined test session ID
     * @param testType the type of test
     * @param isRealtime a sign of the need to recalculate coverage in real time
     * @param testName the name of the test
     * @param isGlobal is the test session global?
     * @param labels the set of labels for marking tests
     * @features Session starting
     */
    fun startSession(
        customSessionId: String?,
        testType: String = "AUTO",
        isRealtime: Boolean = agentConfig.isRealtimeEnable,
        testName: String? = null,
        isGlobal: Boolean = agentConfig.isGlobal,
        labels: Set<Label> = agentConfig.labelCollection,
    ) = runCatching {
        mainLogger.debug { "Attempting to start a Drill4J test session..." }
        val sessionId = customSessionId ?: uuid4().toString()
        val payload = Action.serializer() stringify StartNewSession(
            payload = StartPayload(
                sessionId = sessionId,
                testType = testType,
                testName = testName,
                isRealtime = isRealtime,
                isGlobal = isGlobal,
                labels = labels,
            )
        )
        this.sessionId.value = sessionId
        val response = dispatchAction(payload)
        mainLogger.debug { "Received response: ${response.body}" }
        mainLogger.info { "Started a test session with ID $sessionId" }
    }.onFailure { mainLogger.warn(it) { "Can't startSession '${sessionId.value}'" } }.getOrNull()

    /**
     * Stop a test session
     * @param sessionIds the session ID which need to stop (if not define, current session will be used)
     * @features Session finishing
     */
    fun stopSession(sessionIds: String? = null) = runCatching {
        mainLogger.debug { "Attempting to stop a Drill4J test session..." }
        val payload = Action.serializer() stringify StopSession(
            payload = StopSessionPayload(
                sessionId = sessionIds ?: sessionId.value,
                tests = runCatching {
                    ListSerializer(TestInfo.serializer()) parse TestListener.getData()
                }.getOrNull() ?: emptyList()
            )
        )
        val response = dispatchAction(payload)
        mainLogger.debug { "Received response: ${response.body}" }
        mainLogger.info { "Stopped a test session with ID ${sessionId.value}" }
    }.onFailure {
        mainLogger.warn(it) { "Can't stopSession ${sessionId.value}" }
    }.getOrNull().also { TestListener.reset() }

    private fun dispatchAction(payload: String): HttpResponse {
        val token = token.value.takeIf { it.isNotBlank() } ?: getToken().also {
            token.value = it
        }
        mainLogger.debug { "Auth token: $token" }
        mainLogger.debug {
            """Dispatch action: 
                                |path:$dispatchActionPath
                                |payload:${if (payload.length > 4000) payload.substring(0, 4000) else payload}
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

    /**
     * Send session data to the agents through the admin side
     * @param data session data
     * @features Running tests
     */
    fun sendSessionData(data: String) = runCatching {
        mainLogger.debug { "Attempting to send session data ..." }
        val payload = Action.serializer() stringify AddSessionData(
            payload = SessionDataPayload(
                sessionId = sessionId.value,
                data = data
            )
        )
        val response = dispatchAction(payload)
        mainLogger.debug { "Received response: ${response.body}" }
    }.onFailure {
        mainLogger.warn(it) { "Can't send session data ${sessionId.value}" }
    }.getOrNull().also { TestListener.reset() }

    private fun getToken(): String {
        val httpCall = httpCall(
            agentConfig.adminAddress + "/api/login", HttpRequest(
                "POST",
                body = UserData.serializer() stringify UserData(
                    agentConfig.adminUserName ?: "guest",
                    agentConfig.adminPassword ?: ""
                )
            )
        )
        if (httpCall.code != 200) error("Can't perform request: $httpCall")
        return httpCall.headers["Authorization"] ?: error("No token received during login")
    }
}

@Serializable
data class UserData(
    val name: String,
    val password: String,
)
