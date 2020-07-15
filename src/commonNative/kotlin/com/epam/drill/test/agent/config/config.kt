package com.epam.drill.test.agent.config

import com.epam.drill.test.agent.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import com.epam.drill.logger.api.*
import kotlinx.cinterop.*
import kotlinx.serialization.*
import kotlin.native.concurrent.*

@Serializable
data class AgentRawConfig(
    val agentId: String = "",
    val groupId: String = "",
    val pluginId: String = "",
    val adminAddress: String = "",
    val drillInstallationDir: String = "",
    val logFile: String? = null,
    val logLevel: String = LogLevel.ERROR.name,
    val rawFrameworkPlugins: String = "",
    val sessionId: String? = null,
    val browserProxyAddress: String? = null
) {
    val level: LogLevel
        get() = LogLevel.valueOf(logLevel)

    val frameworkPlugins: List<String>
        get() = rawFrameworkPlugins.split(";")
    val adminHost: String
        get() {
            val url = adminAddress.split(":")
            return if (url.size > 1)
                url[0]
            else adminAddress
        }
    val adminPort: String
        get() {
            val url = adminAddress.split(":")
            return if (url.size > 1)
                url[1]
            else "80"
        }
}

const val WRONG_PARAMS = "Agent parameters are not specified correctly."

fun String?.toAgentParams() = this.asParams().let { params ->
    val result = Properties.load<AgentRawConfig>(params)
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

fun CPointer<JavaVMVar>.initAgent(runtimePath: String) = memScoped {
    initAgentGlobals()
    setUnhandledExceptionHook({ thr: Throwable ->
        thr.printStackTrace()
        mainLogger.error { "Unhandled event $thr" }
    }.freeze())
    val jvmtiCapabilities = alloc<jvmtiCapabilities>()
    jvmtiCapabilities.can_retransform_classes = 1.toUInt()
    jvmtiCapabilities.can_retransform_any_class = 1.toUInt()
    jvmtiCapabilities.can_maintain_original_method_order = 1.toUInt()
    AddCapabilities(jvmtiCapabilities.ptr)
    AddToBootstrapClassLoaderSearch("$runtimePath/drillRuntime.jar")
    callbackRegister()
}

fun CPointer<JavaVMVar>.initAgentGlobals() {
    vmGlobal.value = freeze()
    setJvmti(pointed)
}

private fun setJvmti(vm: JavaVMVar) = memScoped {
    val jvmtiEnvPtr = alloc<CPointerVar<jvmtiEnvVar>>()
    vm.value!!.pointed.GetEnv!!(vm.ptr, jvmtiEnvPtr.ptr.reinterpret(), JVMTI_VERSION.convert())
    jvmti.value = jvmtiEnvPtr.value
    jvmtiEnvPtr.value.freeze()
}