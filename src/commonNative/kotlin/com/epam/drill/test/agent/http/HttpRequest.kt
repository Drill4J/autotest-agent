package com.epam.drill.test.agent.http

class HttpRequest(
    host: String,
    port: String,
    private val path: String,
    body: String,
    private val method: String
) {
    private val protocol = "HTTP/1.1"
    private val lineEnding = "\r\n"
    private val suffix = if (body.isEmpty()) lineEnding else lineEnding + body + lineEnding
    private val headers = mutableMapOf(
        "Host" to "$host:$port",
        "Accept" to "*/*",
        "User-Agent" to "Mozilla/5.0"
    )

    init {
        if (body.isNotBlank()) {
            headers["Content-Length"] = body.length.toString()
        }
    }

    fun addHeader(key: String, value: String) = apply {
        headers[key] = value
    }

    fun build() = headers.map { (key, value) ->
        "$key: $value$lineEnding"
    }.fold("$method $path $protocol$lineEnding") { result, nextHeader ->
        result + nextHeader
    } + suffix
}
