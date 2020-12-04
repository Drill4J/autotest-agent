@file:Suppress("UNUSED_PARAMETER", "UNUSED")

package com.epam.drill.test.agent

import com.epam.drill.hook.io.tcp.injectedHeaders
import com.epam.drill.interceptor.configureHttpInterceptor
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.test.agent.actions.SessionController
import com.epam.drill.test.agent.instrumentation.StrategyManager
import com.epam.drill.test.agent.instrumenting.classFileLoadHookEvent
import kotlinx.cinterop.*
import kotlin.native.concurrent.freeze

fun enableJvmtiEventVmDeath(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, thread)
}

fun enableJvmtiEventVmInit(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, thread)
}

fun enableJvmtiEventClassFileLoadHook(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, thread)
}

@Suppress("UNUSED_PARAMETER")
fun vmDeathEvent(jvmtiEnv: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?) {
    mainLogger.debug { "vmDeathEvent" }
}

fun callbackRegister() = memScoped {
    val eventCallbacks = alloc<jvmtiEventCallbacks>()
    eventCallbacks.VMInit = staticCFunction(::jvmtiEventVMInitEvent)
    eventCallbacks.VMDeath = staticCFunction(::vmDeathEvent)
    eventCallbacks.ClassFileLoadHook = staticCFunction(::classFileLoadHookEvent)
    SetEventCallbacks(eventCallbacks.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    enableJvmtiEventVmInit()
    enableJvmtiEventVmDeath()
}

fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    mainLogger.debug { "Init event" }
    initRuntimeIfNeeded()
    if (!SessionController.agentConfig.isManuallyControlled)
        SessionController.startSession(SessionController.agentConfig.sessionId)
    SessionController.agentConfig.run {
        StrategyManager.initialize(rawFrameworkPlugins, isManuallyControlled)
    }
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)
    configureHooks()
}

fun configureHooks() {
    configureHttpInterceptor()
    mainLogger.debug { "Interceptor configured" }
    injectedHeaders.value = {
        mainLogger.debug { "Injecting headers" }
        val lastTestName = SessionController.testName.value
        val sessionId = SessionController.sessionId.value
        mainLogger.debug { "Adding headers: $lastTestName to $sessionId" }
        mapOf(
            "drill-test-name" to lastTestName,
            "drill-session-id" to sessionId
        )
    }.freeze()
}
