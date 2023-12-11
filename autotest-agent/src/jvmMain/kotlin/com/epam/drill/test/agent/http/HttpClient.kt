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

import com.epam.drill.test.agent.util.*
import java.io.*
import java.net.*
import java.security.*
import java.security.cert.*
import javax.net.ssl.*
import mu.KotlinLogging

object HttpClient {

    private val logger = KotlinLogging.logger {}

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
            HttpResponse(urlConnection.responseCode)
        } finally {
            urlConnection.disconnect()
        }
    }
}
