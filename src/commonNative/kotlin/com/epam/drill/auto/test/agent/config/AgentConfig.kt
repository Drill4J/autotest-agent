package com.epam.drill.auto.test.agent.config

import com.epam.drill.auto.test.agent.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*

data class AgentConfig(
    val agentId: String = "",
    val serviceGroup: String = "",
    val pluginId: String = "",
    val adminHost: String = "",
    val adminPort: String = "",
    val runtimePath: String = "",
    val trace: Boolean = false,
    val debug: Boolean = false,
    val info: Boolean = false,
    val warn: Boolean = false,
    val rawFrameworkPlugins: String = ""
)

const val WRONG_PARAMS = "Agent parameters are not specified correctly."

fun String?.toAgentParams() = this.asParams().let { params ->
    val result = AgentConfig(
        agentId = params["agentId"] ?: "",
        serviceGroup = params["serviceGroup"] ?: "",
        pluginId = params["pluginId"] ?: error(WRONG_PARAMS),
        adminHost = params["adminHost"] ?: error(WRONG_PARAMS),
        adminPort = params["adminPort"] ?: error(WRONG_PARAMS),
        runtimePath = params["runtimePath"] ?: error(WRONG_PARAMS),
        trace = params["trace"]?.toBoolean() ?: false,
        debug = params["debug"]?.toBoolean() ?: false,
        info = params["info"]?.toBoolean() ?: false,
        warn = params["warn"]?.toBoolean() ?: false,
        rawFrameworkPlugins = params["plugins"] ?: ""
    )
    if (result.agentId.isBlank() && result.serviceGroup.isBlank()) {
        error(WRONG_PARAMS)
    }
    result
}

fun String?.asParams(): Map<String, String> = try {
    this?.split(",")?.associate {
        val (key, value) = it.split("=")
        key to value
    } ?: emptyMap()
} catch (parseException: Exception) {
    throw IllegalArgumentException(WRONG_PARAMS)
}

fun CPointer<JavaVMVar>.initAgent(runtimePath: String) = memScoped {
    initAgentGlobals()
    setUnhandledExceptionHook({ thr: Throwable ->
        mainLogger.error { "Unhandled event $thr" }
    }.freeze())
    val jvmtiCapabilities = alloc<jvmtiCapabilities>()
    jvmtiCapabilities.can_retransform_classes = 1.toUInt()
    jvmtiCapabilities.can_retransform_any_class = 1.toUInt()
    jvmtiCapabilities.can_maintain_original_method_order = 1.toUInt()
    AddCapabilities(jvmtiCapabilities.ptr)
    AddToBootstrapClassLoaderSearch("$runtimePath/drillRuntime.jar".apply { println(this) })
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

fun AgentConfig.extractLoggerConfig() = LoggerConfig(trace, debug, info, warn).freeze()