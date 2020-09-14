package com.epam.drill.test.agent.actions

import kotlinx.serialization.*

enum class Actions {
    START,
    STOP
}

@Serializable
data class StartSession(val type: String = Actions.START.name, val payload: StartSessionPayload = StartSessionPayload())

@Serializable
data class StartSessionPayload(
    val sessionId: String = "",
    val testType: String = "AUTO",
    val testName: String? = null,
    val isRealtime: Boolean = false,
    val isGlobal: Boolean = false
)

@Serializable
data class StartSessionResponse(val code: Int, val data: StartSessionResponseData)

@Serializable
data class StartSessionResponseData(val payload: StartSessionPayload)

@Serializable
data class StopSession(val type: String = Actions.STOP.name, val payload: StopPayload)

@Serializable
data class StopPayload(val sessionId: String)

fun stopAction(sessionId: String) = StopSession(
    payload = StopPayload(sessionId)
)