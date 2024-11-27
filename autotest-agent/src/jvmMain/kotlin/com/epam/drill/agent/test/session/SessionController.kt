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
package com.epam.drill.agent.test.session

import com.benasher44.uuid.*
import com.epam.drill.agent.test.configuration.Configuration
import com.epam.drill.agent.test.configuration.ParameterDefinitions
import com.epam.drill.agent.test.testinfo.IntervalTestInfoSender
import com.epam.drill.agent.test.testinfo.TestInfoSender
import com.epam.drill.agent.test.testinfo.TestController
import com.epam.drill.agent.test.transport.TestAgentMessageSender
import mu.KotlinLogging
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

actual object SessionController {
    private val logger = KotlinLogging.logger {}
    private val sessionSender: SessionSender = SessionSenderImpl(
        messageSender = TestAgentMessageSender
    )
    private val testInfoSender: TestInfoSender = IntervalTestInfoSender(
        messageSender = TestAgentMessageSender,
        collectTests = { TestController.getFinishedTests() }
    )
    private lateinit var sessionId: String

    init {
        Runtime.getRuntime().addShutdownHook(Thread { testInfoSender.stopSendingTests() })
    }

    actual fun startSession() {
        val customSessionId = Configuration.parameters[ParameterDefinitions.SESSION_ID]
        sessionId = customSessionId.takeIf(String::isNotBlank) ?: uuid4().toString()
        logger.info { "Test session started: $sessionId" }
        sessionSender.sendSession(
            SessionPayload(
                id = sessionId,
                groupId = Configuration.parameters[ParameterDefinitions.GROUP_ID],
                testTaskId = Configuration.parameters[ParameterDefinitions.TEST_TASK_ID],
                startedAt = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            )
        )
        testInfoSender.startSendingTests()
    }

    fun getSessionId(): String = sessionId
}