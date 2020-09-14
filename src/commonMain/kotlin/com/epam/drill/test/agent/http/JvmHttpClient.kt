package com.epam.drill.test.agent.http

import com.epam.drill.test.agent.config.parse
import com.epam.drill.test.agent.config.stringify

expect object JvmHttpClient {
    fun httpCall(endpoint: String, request: String): String
}

fun httpCall(endpoint: String, request: HttpRequest): HttpResponse {
    return HttpResponse.serializer() parse JvmHttpClient.httpCall(endpoint, HttpRequest.serializer() stringify request)
}