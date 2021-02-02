package com.epam.drill.test.agent.js

import kotlinx.serialization.*
import kotlinx.serialization.json.*


@Serializable
data class WsMessage(
    val type: EventType,
    val from: Client,
    val payload: JsonElement = JsonPrimitive("")
)

@Serializable
data class Client(
    val id: String,
    val type: String = "autotest-agent"
)

@Serializable
data class Session(
    val sessionId: String,
    val testName: String
)

@Serializable
enum class EventType {
    START_TEST,
    FINISH_TEST,
    CONNECT,
    READY
}
