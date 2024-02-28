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
package com.epam.drill.test.agent.session

import com.benasher44.uuid.*
import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.configuration.*
import com.epam.drill.test.agent.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.concurrent.thread
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.ResponseStatus
import com.epam.drill.test.agent.transport.AdminMessageSender
import mu.KotlinLogging

actual object SessionController {
    var testHash = "undefined"
    var sessionId = ""
    private val logger = KotlinLogging.logger("com.epam.drill.test.agent.actions.SessionController")

    private val dispatchActionDestination = AgentMessageDestination(
        "POST",
        "plugins/${Configuration.parameters[ParameterDefinitions.PLUGIN_ID]}/dispatch-action"
    )

    init {
        thread {
            while (true) {
                Thread.sleep(3000)
                runCatching {
                    val tests = runCatching {
                        json.decodeFromString(ListSerializer(TestInfo.serializer()), TestListener.getData())
                    }.getOrNull() ?: emptyList()
                    if (tests.any()) {
                        sendTests(tests)
                    }
                }.onFailure { logger.error(it) { "Can't parse tests. Reason:" } }
            }
        }
    }

    actual fun startSession(customSessionId: String?) {
        startSession(customSessionId, "AUTO")
    }

    actual fun stopSession() {
        stopSession(null)
    }

    private fun sendTests(tests: List<TestInfo>) {
        val payload = AddTests(payload = AddTestsPayload(ThreadStorage.sessionId(), tests))
        val result = dispatchAction(payload)
        logger.trace { "Count of tests sent: ${tests.size}, received status ${result.statusObject}" }
    }

    fun startSession(
        customSessionId: String?,
        testType: String = "AUTO",
        isRealtime: Boolean = Configuration.parameters[ParameterDefinitions.IS_REALTIME_ENABLED],
        testName: String? = null,
        isGlobal: Boolean = Configuration.parameters[ParameterDefinitions.IS_GLOBAL],
        labels: Set<Label> = Configuration.parameters[ParameterDefinitions.LABELS],
    ) = runCatching {
        logger.debug { "Attempting to start a Drill4J test session..." }
        val sessionId = customSessionId ?: uuid4().toString()
        val payload = StartNewSession(payload = StartPayload(
            sessionId = sessionId,
            testType = testType,
            testName = testName,
            isRealtime = isRealtime,
            isGlobal = isGlobal,
            labels = labels,
        ))
        SessionController.sessionId = sessionId
        val response = dispatchAction(payload)
        logger.debug { "Received response: ${response.statusObject}" }
        logger.info { "Started a test session with ID $sessionId" }
    }.onFailure { logger.warn(it) { "Can't startSession '$sessionId'" } }.getOrNull()

    fun stopSession(sessionIds: String? = null) = runCatching {
        logger.debug { "Attempting to stop a Drill4J test session..." }
        val payload = StopSession(payload = StopSessionPayload(
            sessionId = sessionIds ?: sessionId,
            tests = runCatching {
                json.decodeFromString(ListSerializer(TestInfo.serializer()), TestListener.getData())
            }.getOrNull() ?: emptyList()
        ))
        val response = dispatchAction(payload)
        logger.debug { "Received response: ${response.statusObject}" }
        logger.info { "Stopped a test session with ID $sessionId" }
    }.onFailure {
        logger.warn(it) { "Can't stopSession $sessionId" }
    }.getOrNull().also { TestListener.reset() }

    private fun dispatchAction(payload: Action): ResponseStatus {
        logger.debug {
            """Dispatch action: 
                                |path:${dispatchActionDestination.target}
                                |payload:${payload}
                                |""".trimMargin()
        }
        return AdminMessageSender.send(dispatchActionDestination, payload).also {
            if (!it.success) error("Can't perform request: ${it.statusObject}")
        }
    }

    fun sendSessionData(data: String) = runCatching {
        logger.debug { "Attempting to send session data ..." }
        val payload = AddSessionData(payload = SessionDataPayload(sessionId = sessionId, data = data))
        val response = dispatchAction(payload)
        logger.debug { "Received response: ${response.statusObject}" }
    }.onFailure {
        logger.warn(it) { "Can't send session data $sessionId" }
    }.getOrNull().also { TestListener.reset() }
}
