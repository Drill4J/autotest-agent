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

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import mu.KotlinLogging

private const val CONTENT_TYPE = "Content-Type"
private const val APPLICATION_JSON = "application/json"

data class HttpUrlConnectionBuilder(
    var url: String,
    val queryParams: MutableMap<String, String> = mutableMapOf(),
    var method: HttpMethod = HttpMethod.GET,
    val headers: MutableMap<String, String> = mutableMapOf(),
    var body: String = "",
    var timeout: Int = 15_000,
) {

    private val logger = KotlinLogging.logger {}

    fun build(): HttpURLConnection = (URL(url + queryParamString()).openConnection() as HttpURLConnection).apply {
        requestMethod = method.name
        doOutput = true
        connectTimeout = timeout
        readTimeout = 30_000
        setRequestProperty(CONTENT_TYPE, APPLICATION_JSON)
        headers.forEach { (key, value) ->
            setRequestProperty(key, value)
        }
        if (body.isNotBlank()) {
            outputStream.use { it.write(body.toByteArray()) }
        }
        logger.trace {
            """Trying to execute request $method
            | path: $url
            | body: $body     
        """.trimMargin()
        }
    }

    private fun queryParamString() = queryParams.entries.joinToString(separator = "&", prefix = "?") {
        val urlEncode: (String) -> String = { URLEncoder.encode(it, Charsets.UTF_8.name()) }
        "${urlEncode(it.key)}=${urlEncode(it.value)}"
    }
}
