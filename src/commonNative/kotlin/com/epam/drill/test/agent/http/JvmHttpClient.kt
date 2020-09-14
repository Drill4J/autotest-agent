package com.epam.drill.test.agent.http


actual object JvmHttpClient {
    actual fun httpCall(endpoint: String, request: String): String {
        return JvmHttpClientStub.httpCall(endpoint, request)
    }
}