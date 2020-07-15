package com.epam.drill.test.agent.actions

import kotlinx.serialization.*

enum class Actions {
    START,
    STOP
}

@Serializable
data class StartSession(val type: String = Actions.START.name, val payload: StartPayload = StartPayload())

@Serializable
data class StartPayload(val testType: String = "AUTO", val sessionId: String = "")

@Serializable
data class StartSessionResponse(val code: Int, val data: StartSessionResponseData)

@Serializable
data class StartSessionResponseData(val type: String = "", val payload: StartResponsePayload)

@Serializable
data class StartResponsePayload(val sessionId: String, val startPayload: StartPayload)

@Serializable
data class StopSession(val type: String = Actions.STOP.name, val payload: StopPayload)

@Serializable
data class StopPayload(val sessionId: String)

fun stopAction(sessionId: String) = StopSession(
    payload = StopPayload(sessionId)
)