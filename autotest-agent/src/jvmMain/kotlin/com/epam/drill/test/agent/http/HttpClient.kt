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

import com.epam.drill.logger.*
import com.epam.drill.test.agent.util.*
import java.io.*
import java.net.*
import java.security.*
import java.security.cert.*
import javax.net.ssl.*

private const val CONTENT_TYPE = "Content-Type"
private const val APPLICATION_JSON = "application/json"

object HttpClient {
    val logger = Logging.logger(HttpClient::class.java.name)

    init {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>,
                    authType: String,
                ) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(
                    chain: Array<X509Certificate>,
                    authType: String,
                ) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        )
        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
    }

    fun request(
        url: String,
        block: HttpUrlConnectionBuilder.() -> Unit = { }
    ): HttpResponse = HttpUrlConnectionBuilder(url).apply(block).build().let { urlConnection ->
        try {
            urlConnection.connect()
            HttpResponse(
                urlConnection.responseCode,
                urlConnection.headerFields.mapValues { it.value.first() },
                urlConnection.inputStream.reader().use { it.readText() }
            )
        } catch (ex: IOException) {
            logger.error(ex) { "Can't get response from $url" }
            HttpResponse(500)
        } finally {
            urlConnection.disconnect()
        }
    }
}

data class HttpUrlConnectionBuilder(
    var url: String,
    val queryParams: MutableMap<String, String> = mutableMapOf(),
    var method: HttpMethod = HttpMethod.GET,
    val headers: MutableMap<String, String> = mutableMapOf(),
    var body: String = "",
    var timeout: Int = 15_000,
) {

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
        "${it.key.urlEncode()}=${it.value.urlEncode()}"
    }
}

enum class HttpMethod {
    GET, POST, DELETE, PUT, PATCH
}
