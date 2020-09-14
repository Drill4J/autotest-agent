package com.epam.drill.test.agent.http

import com.epam.drill.kni.Kni
import com.epam.drill.test.agent.config.parse
import com.epam.drill.test.agent.config.stringify
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*


@Kni
actual object JvmHttpClient {

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        return try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate>,
                        authType: String
                    ) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(
                        chain: Array<X509Certificate>,
                        authType: String
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
            val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
            builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    actual fun httpCall(endpoint: String, request: String): String {
        val httpRequest = HttpRequest.serializer() parse request
        return kotlin.runCatching {
            val response = getUnsafeOkHttpClient().newCall(
                okhttp3.Request.Builder()
                    .url(if (endpoint.startsWith("http")) endpoint else "http://$endpoint")
                    .method(httpRequest.method, RequestBody.create(MediaType.get("application/json"), httpRequest.body))
                    .headers(Headers.of(httpRequest.headers))
                    .build()
            ).execute()
            val httpResponse = HttpResponse(
                response.code(),
                response.headers().toMultimap().map { it.key to it.value.first() }.associate { it.first to it.second },
                response.body()!!.byteStream().reader().readText()
            )
            HttpResponse.serializer() stringify httpResponse
        }.onFailure { it.printStackTrace() }.getOrDefault(HttpResponse.serializer() stringify HttpResponse(500))
    }
}