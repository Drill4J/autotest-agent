@file:Suppress("UNUSED_PARAMETER", "UNUSED")

package com.epam.drill.test.agent

import com.epam.drill.jvmapi.gen.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.test.agent.actions.*
import com.epam.drill.test.agent.config.*
import kotlinx.cinterop.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.native.concurrent.*

@SharedImmutable
val mainLogger = Logging.logger("AutoTestAgentLogger")

object Agent : JvmtiAgent {

    override fun agentOnLoad(options: String): Int = memScoped {
        try {
            val config = options.toAgentParams().freeze()
            setUnhandledExceptionHook({ thr: Throwable ->
                mainLogger.error(thr) { "Unhandled event $thr" }
            }.freeze())
            val jvmtiCapabilities = alloc<jvmtiCapabilities>()
            jvmtiCapabilities.can_retransform_classes = 1.toUInt()
            jvmtiCapabilities.can_retransform_any_class = 1.toUInt()
            jvmtiCapabilities.can_maintain_original_method_order = 1.toUInt()
            AddCapabilities(jvmtiCapabilities.ptr)
            AddToBootstrapClassLoaderSearch("${config.drillInstallationDir}/drillRuntime.jar")
            callbackRegister()

            SessionController._agentConfig.value = config
        } catch (ex: Throwable) {
            mainLogger.error(ex) { "Can't load the agent. Reason:" }
        }
        return JNI_OK
    }

    override fun agentOnUnload() {
        try {
            mainLogger.info { "Shutting the agent down" }
            SessionController.stopSession()
        } catch (ex: Throwable) {
            mainLogger.error { "Failed to unload the agent properly. Reason: ${ex.message}" }
        }
    }

}

const val WRONG_PARAMS = "Agent parameters are not specified correctly."

fun String?.toAgentParams() = this.asParams().let { params ->
    val result = AgentRawConfig.serializer().deserialize(StringPropertyDecoder(params))
    println(result)
    if (result.agentId.isBlank() && result.groupId.isBlank()) {
        error(WRONG_PARAMS)
    }
    Logging.filename = result.logFile
    Logging.logLevel = result.level
    result
}

fun String?.asParams(): Map<String, String> = try {
    this?.split(",")?.filter { it.isNotEmpty() }?.associate {
        val (key, value) = it.split("=")
        key to value
    } ?: emptyMap()
} catch (parseException: Exception) {
    throw IllegalArgumentException(WRONG_PARAMS)
}


