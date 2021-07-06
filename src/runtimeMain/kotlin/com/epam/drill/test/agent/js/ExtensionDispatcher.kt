package com.epam.drill.test.agent.js

import com.epam.drill.test.agent.*
import java.net.*

object ExtensionDispatcher {
    private var wsClient: ExtensionDispatcherWs? = null

    init {
        AgentConfig.dispatcherUrl()?.run {
            wsClient = ExtensionDispatcherWs(URI("ws://$this")).apply {
                connect()
                await()
                send(EventType.CONNECT)
            }
        }
    }

    fun send(eventType: EventType) {
        wsClient?.send(eventType)
        wsClient?.await()
    }

    fun await() = wsClient?.await()

    fun clientId() = wsClient?.id
}
