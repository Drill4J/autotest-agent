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
package com.epam.drill.test.agent.http

import com.epam.drill.kni.*
import com.epam.drill.test.agent.config.*
import java.net.*
import mu.KotlinLogging


@Kni
actual object JvmHttpClient {

    private val logger = KotlinLogging.logger {}

    actual fun httpCall(endpoint: String, request: String): String {
        val httpRequest = HttpRequest.serializer() parse request
        return runCatching {
            val httpResponse = HttpClient.request(if (endpoint.startsWith("http")) endpoint else "http://$endpoint") {
                method = HttpMethod.valueOf(httpRequest.method)
                headers += httpRequest.headers
                body = httpRequest.body
            }
            HttpResponse.serializer() stringify httpResponse
        }.onFailure {
            if (it is SocketTimeoutException) {
                logger.warn { "Can't get response to request: $request. Read time out" }
            } else logger.error(it) { "Can't get response. Reason:" }
        }.getOrDefault(HttpResponse.serializer() stringify HttpResponse(500))
    }
}
