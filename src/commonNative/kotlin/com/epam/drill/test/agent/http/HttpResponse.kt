package com.epam.drill.test.agent.http

import com.epam.drill.test.agent.*

class HttpResponse(raw: String) {
    val headers = raw.lines().map { line ->
        line.substringBefore(':').trim() to line.substringAfter(':').trim()
    }.toMap()

    val body: String

    init {
        val contentLength = headers["Content-Length"]?.toInt()
            ?: 0.apply { mainLogger.info { "No Content-Length header in response - assuming body's length 0" } }
        body = raw.substringAfter("\r\n\r\n").substring(0 until contentLength)
    }
}
