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
package com.epam.drill.test.agent.testinfo

import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.AgentMessageSender
import com.epam.drill.plugins.test2code.api.AddTestsPayload
import com.epam.drill.plugins.test2code.api.TestInfo
import com.epam.drill.test.agent.session.SessionController
import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

interface TestInfoSender {
    fun startSendingTests()
    fun stopSendingTests()
}

class IntervalTestInfoSender(
    private val messageSender: AgentMessageSender<AgentMessage>,
    private val intervalMs: Long = 1000,
    private val collectTests: () -> List<TestInfo> = { emptyList() }
) : TestInfoSender {
    private val logger = KotlinLogging.logger {}
    private val scheduledThreadPool = Executors.newSingleThreadScheduledExecutor()

    override fun startSendingTests() {
        scheduledThreadPool.scheduleAtFixedRate(
            { sendTests(collectTests()) },
            0,
            intervalMs,
            TimeUnit.MILLISECONDS
        )
        logger.debug { "Test sending job is started." }
    }

    override fun stopSendingTests() {
        scheduledThreadPool.shutdown()
        if (!scheduledThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
            logger.error("Failed to send some tests prior to shutdown")
            scheduledThreadPool.shutdownNow();
        }
        sendTests(collectTests())
        logger.info { "Test sending job is stopped." }
    }

    private fun sendTests(tests: List<TestInfo>) {
        if (tests.isEmpty()) return
        messageSender.send(
            destination = AgentMessageDestination("POST", "tests-metadata"),
            message = AddTestsPayload(
                sessionId = SessionController.getSessionId(),
                tests = tests
            )
        )
    }
}