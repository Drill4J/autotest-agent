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
import com.epam.drill.test.agent.serialization.*
import kotlinx.serialization.builtins.*
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.ResponseStatus
import com.epam.drill.test.agent.transport.AdminMessageSender
import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

actual object SessionController {
    var testHash = "undefined"
    var sessionId = ""
    private val logger = KotlinLogging.logger("com.epam.drill.test.agent.actions.SessionController")
    private val scheduledThreadPool = Executors.newSingleThreadScheduledExecutor()
    private val intervalMs: Long = 1000

    init {
        scheduledThreadPool.scheduleAtFixedRate(
            { getAndSendTests() },
            0,
            intervalMs,
            TimeUnit.MILLISECONDS
        )
    }

    actual fun startSession(customSessionId: String) {
        runCatching {
            val sessionId = customSessionId.takeIf(String::isNotBlank) ?: uuid4().toString()
            SessionController.sessionId = sessionId
        }.getOrNull()
            .also { TestListener.reset() }
    }

    actual fun stopSession() {
        runCatching {
            getAndSendTests()
        }.getOrNull()
            .also {  TestListener.reset() }
    }

    private fun sendTests(tests: List<TestInfo>) = runCatching {
        val addTestsPayload = AddTestsPayload(sessionId = sessionId, tests)
        sendToAdmin(
            destination = AgentMessageDestination(
                "POST",
                "tests-metadata"
            ),
            payload = addTestsPayload
        )
    }.onFailure {
        logger.warn(it) { "can't send test metadata by session $sessionId" }
    }

    fun sendSessionData(data: String) = runCatching {
        val payload = AddSessionData(sessionId = sessionId, data = data)
        sendToAdmin(
            AgentMessageDestination(
                "POST",
                "raw-javascript-coverage"
            ),
            payload
        )
    }.onFailure {
        logger.warn(it) { "can't send js raw coverage $sessionId" }
    }.getOrNull().also { TestListener.reset() }

    private fun sendToAdmin(destination: AgentMessageDestination, payload: Action): ResponseStatus {
        return AdminMessageSender.send(destination, payload)
            .also {
                if (!it.success) error("request ${destination.target} failed with ${it.statusObject}")
            }
    }


    private fun getAndSendTests() {
        runCatching {
            val tests = runCatching {
                json.decodeFromString(ListSerializer(TestInfo.serializer()), TestListener.retrieveData())
            }.getOrNull() ?: emptyList()
            if (tests.any()) {
                sendTests(tests)
            }
        }.onFailure { logger.error(it) { "Can't parse tests. Reason:" } }
    }

}