package com.epam.drill.test.agent.instrumentation.http.selenium

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class TargetInfos(val targetInfos: List<Target>)

@Serializable
data class SessionId(val sessionId: String = "")

@Serializable
data class Target(
    val targetId: String,
    val type: String,
    val title: String,
    val url: String,
    val attached: Boolean,
    val browserContextId: String,
)

@Serializable
sealed class DevToolsMessage {
    abstract val target: String
    abstract val sessionId: String
}

@Serializable
data class DevToolsRequest(
    override val target: String,
    override val sessionId: String = "",
    val params: Map<String, JsonElement> = emptyMap()
) : DevToolsMessage()

@Serializable
data class DevToolsHeaderRequest(
    override val target: String,
    override val sessionId: String,
    val params: Map<String, Map<String, String>> = emptyMap(),
) : DevToolsMessage()
