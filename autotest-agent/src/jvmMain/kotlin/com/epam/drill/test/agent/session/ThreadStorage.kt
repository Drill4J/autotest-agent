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

import com.epam.drill.common.agent.request.DrillRequest
import com.epam.drill.common.agent.request.RequestHolder
import com.epam.drill.test.agent.TEST_ID_HEADER
import java.net.*
import com.epam.drill.test.agent.serialization.json
import mu.KotlinLogging

object ThreadStorage : RequestHolder {
    private val logger = KotlinLogging.logger {}
    private var threadStorage: InheritableThreadLocal<DrillRequest> =  InheritableThreadLocal()

    @Suppress("unused")
    fun memorizeTestName(testName: String?) {
        val value = testName?.let { URLEncoder.encode(it, Charsets.UTF_8.name()) }
        SessionController.testHash = value ?: ""
        store(DrillRequest(
            drillSessionId = sessionId(),
            headers = mapOf(TEST_ID_HEADER to (value ?: "unspecified"))
        ))
    }

    fun clear() {
        remove()
    }

    fun sessionId(): String {
        return SessionController.sessionId
    }

    fun startSession() {

    }

    fun stopSession() = SessionController.run {

    }

    fun sendSessionData(preciseCoverage: String, scriptParsed: String, testId: String) {
        val data = SessionData(preciseCoverage, scriptParsed, testId)
        SessionController.sendSessionData(json.encodeToString(SessionData.serializer(), data))
    }

    override fun remove() {
        if (threadStorage.get() == null) return
        logger.trace { "remove: Request ${threadStorage.get().drillSessionId} removed, threadId = ${Thread.currentThread().id}" }
        threadStorage.remove()
    }

    override fun retrieve(): DrillRequest? =
        threadStorage.get()

    override fun store(drillRequest: DrillRequest) {
        threadStorage.set(drillRequest)
        logger.trace { "store: Request ${drillRequest.drillSessionId} saved, threadId = ${Thread.currentThread().id}" }
    }

    fun retrieveTestLaunchId(): String? = retrieve()?.headers?.get(TEST_ID_HEADER)

}
