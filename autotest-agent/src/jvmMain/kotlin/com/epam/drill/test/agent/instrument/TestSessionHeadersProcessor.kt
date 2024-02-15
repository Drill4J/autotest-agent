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
package com.epam.drill.test.agent.instrument

import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.test.agent.SESSION_ID_HEADER
import com.epam.drill.test.agent.TEST_ID_HEADER
import com.epam.drill.test.agent.session.ThreadStorage

object TestSessionHeadersProcessor : HeadersProcessor {

    override fun removeHeaders() = Unit

    override fun storeHeaders(headers: Map<String, String>) = Unit

    override fun retrieveHeaders() = mutableMapOf<String, String>().apply {
        ThreadStorage.sessionId()?.let {
            put(SESSION_ID_HEADER, it)
        }
        ThreadStorage.storage.get()?.let {
            put(TEST_ID_HEADER, it)
        }
    }

    override fun hasHeaders() = retrieveHeaders().run {
        this.isNotEmpty() && this.get(SESSION_ID_HEADER) != null && this.get(TEST_ID_HEADER) != null
    }

    override fun isProcessRequests() = true

    override fun isProcessResponses() = false

}
