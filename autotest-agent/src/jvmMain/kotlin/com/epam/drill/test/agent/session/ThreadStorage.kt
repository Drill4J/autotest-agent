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
import mu.KotlinLogging

object ThreadStorage : RequestHolder {
    private val logger = KotlinLogging.logger {}
    private var threadStorage: InheritableThreadLocal<DrillRequest> =  InheritableThreadLocal()

    @Suppress("unused")
    fun storeTestLaunchId(testLaunchId: String) {
        store(DrillRequest(
            drillSessionId = SessionController.getSessionId(),
            headers = mapOf(TEST_ID_HEADER to (testLaunchId))
        ))
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

    @Deprecated("Use explicit retrieve() instead", ReplaceWith("retrieve()"))
    fun getTestLaunchId(): String? = retrieve()?.headers?.get(TEST_ID_HEADER)

}
