package com.epam.drill.test.agent.http

import com.epam.drill.test.agent.*
import com.epam.drill.internal.socket.socket_get_error
import com.epam.drill.transport.net.resolveAddress
import kotlinx.cinterop.*
import platform.posix.*

object Sender {

    fun post(
        host: String,
        port: String,
        path: String,
        headers: Map<String, String> = emptyMap(),
        body: String = "",
        responseBufferSize: Int = 4096
    ): HttpResponse {
        val request = HttpRequest(host, port, path, body, "POST")
        headers.forEach { (key, value) ->
            request.addHeader(key, value)
        }
        return httpRequest(
            host,
            port,
            request.build(),
            responseBufferSize
        )
    }

    private fun httpRequest(
        host: String,
        port: String,
        request: String,
        responseBufferSize: Int = 4096
    ): HttpResponse = memScoped {
        val sfd = connect(host, port)
        val requestLength = request.length
        mainLogger.debug { "Attempting to send request of length $requestLength" }
        val written = send(sfd.convert(), request.cstr, requestLength.convert(), 0)
        mainLogger.debug { "Wrote $written of $requestLength expected; error: ${socket_get_error()}" }
        val buffer = " ".repeat(responseBufferSize).cstr.getPointer(memScope)
        val read = recv(sfd.convert(), buffer, responseBufferSize.convert(), 0)
        mainLogger.debug { "Read $read of $responseBufferSize possible" }
        val result = buffer.toKString()
        close(sfd.convert())
        mainLogger.debug { "Closed socket connection" }
        return HttpResponse(result)
    }

    private fun connect(host: String, port: String): ULong =
        socket(AF_INET.convert(), SOCK_STREAM.convert(), IPPROTO_TCP.convert()).also { socketfd ->
            @Suppress("UNCHECKED_CAST")
            connect(socketfd.convert(), resolveAddress(host, port.toInt()) as CValuesRef<sockaddr>, sockaddr_in.size.convert())
        }.convert()

}
