@file:Suppress("UNUSED_PARAMETER", "UNUSED")

package com.epam.drill.test.agent

import com.epam.drill.test.agent.actions.*
import com.epam.drill.test.agent.config.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*

@SharedImmutable
val mainLogger = Logging.logger("AutoTestAgentLogger")

@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: CPointer<JavaVMVar>, options: String, reservedPtr: Long): jint = memScoped {
    try {
        val agentConfig = options.toAgentParams().freeze()
        vmPointer.initAgent(agentConfig.drillInstallationDir)

        SessionController.agentConfig.value = agentConfig
        SessionController.startSession(agentConfig.sessionId)
    } catch (ex: Throwable) {
        mainLogger.error(ex) { "Can't load the agent. Reason:" }
    }
    JNI_OK
}

@CName("Agent_OnUnload")
fun agentOnUnload(vmPointer: CPointer<JavaVMVar>) {
    try {
        mainLogger.info { "Shutting the agent down" }
        SessionController.stopSession()
    } catch (ex: Throwable) {
        mainLogger.error { "Failed to unload the agent properly. Reason: ${ex.message}" }
    }
}
