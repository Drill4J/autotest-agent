package com.epam.drill.auto.test.agent.actions

import com.epam.drill.auto.test.agent.*
import com.epam.drill.auto.test.agent.config.*
import com.epam.drill.auto.test.agent.http.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*

object SessionController {
    val agentConfig = AtomicReference(AgentConfig().freeze()).freeze()
    val testName = AtomicReference("")
    val sessionId = AtomicReference("")

    private val dispatchActionPath: String
        get() = if (agentConfig.value.serviceGroup.isBlank()) {
            "/api/agents/${agentConfig.value.agentId}/plugins/${agentConfig.value.pluginId}/dispatch-action"
        } else "/api/service-group/${agentConfig.value.serviceGroup}/plugins/${agentConfig.value.pluginId}/dispatch-action"

    fun startSession() {
        mainLogger.debug { "Attempting to start a Drill4J test session..." }
        val payload = StartSession.serializer() stringify StartSession()
        val response = dispatchAction(payload)
        mainLogger.debug { "Received response: ${response.body}" }
        val startSessionResponse = StartSessionResponse.serializer() parse response.body
        sessionId.value = startSessionResponse.data.payload.sessionId
        mainLogger.info { "Started a test session with ID ${sessionId.value}" }
    }

    fun stopSession() {
        mainLogger.debug { "Attempting to stop a Drill4J test session..." }
        val payload = StopSession.serializer() stringify stopAction(sessionId.value)
        val response = dispatchAction(payload)
        mainLogger.debug { "Received response: ${response.body}" }
        mainLogger.info { "Stopped a test session with ID ${sessionId.value}" }
    }

    private fun dispatchAction(payload: String): HttpResponse {
        val token = getToken()
        return Sender.post(
            agentConfig.value.adminHost,
            agentConfig.value.adminPort,
            dispatchActionPath,
            mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            payload
        )
    }

    private fun getToken(): String = Sender.post(
        agentConfig.value.adminHost,
        agentConfig.value.adminPort,
        "/api/login"
    ).headers["Authorization"] ?: error("No token received during login")

}

@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Java_com_epam_drill_auto_test_agent_AgentClassTransformer_memorizeTestName")
fun memorizeTestName(env: CPointer<JNIEnvVar>?, thisObj: jobject, inJNIStr: jstring) {
    val testNameFromJava: String =
        env?.pointed?.pointed?.GetStringUTFChars?.invoke(env, inJNIStr, null)?.toKString() ?: ""
    SessionController.testName.value = testNameFromJava
}
