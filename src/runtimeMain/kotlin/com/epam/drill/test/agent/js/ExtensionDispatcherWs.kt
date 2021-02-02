package com.epam.drill.test.agent.js

import com.epam.drill.logger.*
import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.config.*
import kotlinx.atomicfu.*
import kotlinx.collections.immutable.*
import org.java_websocket.client.*
import org.java_websocket.handshake.*
import java.net.*
import java.util.*
import kotlin.concurrent.*
import kotlin.time.*

private val logger = Logging.logger(ExtensionDispatcherWs::class.java.name)

class ExtensionDispatcherWs(url: URI) : WebSocketClient(url) {
    private val signal = atomic(Signal(120.seconds))

    val id = UUID.randomUUID().toString()

    override fun onOpen(handshakedata: ServerHandshake?) {
        logger.trace { "Ws was opened" }
        signal.value.received("connected")
    }

    override fun onMessage(message: String) {
        logger.trace { "Received message: $message" }
        signal.value.received(message)
    }

    override fun onError(ex: java.lang.Exception?) {
        logger.error(ex) { "Socket closed by error:" }
    }

    fun send(event: EventType) = runCatching {
        when (event) {
            EventType.CONNECT -> {
                val message = WsMessage(event, Client(id))
                send(WsMessage.serializer() stringify message)
                logger.trace { "Subscribed to extension proxy" }
            }
            else -> {
                val jsonElement = json.encodeToJsonElement(
                    serializer = Session.serializer(),
                    value = Session(
                        sessionId = ThreadStorage.sessionId().orEmpty(),
                        testName = ThreadStorage.testName().orEmpty()
                    )
                )
                val message = WsMessage(event, Client(id), jsonElement)
                send(WsMessage.serializer() stringify message)
                logger.trace { "Sent message: $message" }
            }
        }
    }.getOrElse { logger.warn(it) { "Failed to send a message. Reason:" } }


    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        logger.debug { "Socket closed. Code: $code, reason: $reason, remote: $remote" }
    }

    fun await() = signal.value.await()
}

class Signal(private val timeout: Duration) {
    private val _messages = atomic(persistentListOf<String>())

    private val state: Boolean
        get() = _messages.value.isEmpty()

    fun received(message: String) {
        _messages.update { it + message }
        logger.trace { "Signal received: $message" }
    }

    fun await() {
        if (state) {
            awaitWithExpr(timeout) { state }
        }
        _messages.update { it.clear() }
    }

    private fun awaitWithExpr(timeout: Duration, state: () -> Boolean) = thread(true) {
        logger.info { "Await signal..." }
        val expirationMark = TimeSource.Monotonic.markNow() + timeout
        while (state()) {
            if (expirationMark.hasPassedNow()) {
                throw RuntimeException("Haven't received a signal in $timeout")
            }
            Thread.sleep(200)
        }
    }.join()
}
