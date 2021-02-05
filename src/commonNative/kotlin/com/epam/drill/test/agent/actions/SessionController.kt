/**
 * Copyright 2020 EPAM Systems
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
import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.config.*
import com.epam.drill.test.agent.http.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.*
import kotlin.time.seconds as sec

object SessionController {
    val _agentConfig = AtomicReference(AgentRawConfig().freeze()).freeze()
    val agentConfig
        get() = _agentConfig.value
    val testName = AtomicReference("undefined")
    val sessionId = AtomicReference("")

    private val dispatchActionPath: String
        get() = if (agentConfig.groupId.isBlank()) {
            "/api/agents/${agentConfig.agentId}/plugins/${agentConfig.pluginId}/dispatch-action"
        } else "/api/groups/${agentConfig.groupId}/plugins/${agentConfig.pluginId}/dispatch-action"

    init {
        GlobalScope.launch {
            while (true) {
                delay(3.sec)
                runCatching {
                    val testRun = TestRun.serializer() parse TestListener.getData()
                    if (testRun.tests.any()) {
                        sendTests(testRun)
                    }
                }.onFailure { mainLogger.error(it) { "Can't parse tests. Reason:" } }
            }
        }
    }

    private fun sendTests(tests: TestRun) {
        val payload = AddTests.serializer() stringify AddTests(
            payload = AddTestsPayload(ThreadStorage.sessionId() ?: "", tests)
        )
        val result = dispatchAction(payload)
        mainLogger.trace { "Sent tests, received status ${result.code}" }
    }

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
    }.onFailure {
        mainLogger.warn(it) { "Can't stopSession ${sessionId.value}" }
    }.getOrNull().also { TestListener.reset() }

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
