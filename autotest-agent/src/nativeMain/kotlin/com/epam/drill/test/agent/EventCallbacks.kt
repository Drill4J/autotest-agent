/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("UNUSED_PARAMETER", "UNUSED")

package com.epam.drill.test.agent

import com.epam.drill.jvmapi.callObjectVoidMethod
import com.epam.drill.jvmapi.callObjectVoidMethodWithInt
import com.epam.drill.jvmapi.callObjectVoidMethodWithString
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logging.LoggingConfiguration
import com.epam.drill.test.agent.actions.SessionController
import com.epam.drill.test.agent.instrumentation.StrategyManager
import com.epam.drill.test.agent.instrumenting.classFileLoadHookEvent
import kotlinx.cinterop.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger("com.epam.drill.test.agent.EventCallbacks")

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
    logger.debug { "vmDeathEvent" }
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

private const val isHttpHookEnabled = false // based on args

fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    logger.debug { "Init event" }
    initRuntimeIfNeeded()
    val agentConfig = AgentConfig.config
    if (!agentConfig.isManuallyControlled && !agentConfig.sessionForEachTest)
        SessionController.startSession(agentConfig.sessionId)
    agentConfig.run {
        StrategyManager.initialize(rawFrameworkPlugins, isManuallyControlled)
        callObjectVoidMethod(LoggingConfiguration::class, LoggingConfiguration::readDefaultConfiguration.name)
        callObjectVoidMethodWithString(LoggingConfiguration::class, "setLoggingLevels", logLevel)
        callObjectVoidMethodWithString(LoggingConfiguration::class, LoggingConfiguration::setLoggingFilename, logFile)
        callObjectVoidMethodWithInt(LoggingConfiguration::class, LoggingConfiguration::setLogMessageLimit, logLimit)
    }
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)
    if (isHttpHookEnabled)
        configureHooks()
}

fun configureHooks() {
    logger.debug { "Drill interceptor is unavailable" }

}
