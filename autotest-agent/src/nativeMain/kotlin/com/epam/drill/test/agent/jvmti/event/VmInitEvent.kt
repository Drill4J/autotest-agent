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
package com.epam.drill.test.agent.jvmti.event

import kotlinx.cinterop.CPointer
import mu.KotlinLogging
import com.epam.drill.jvmapi.callObjectVoidMethod
import com.epam.drill.jvmapi.callObjectVoidMethodWithInt
import com.epam.drill.jvmapi.callObjectVoidMethodWithString
import com.epam.drill.jvmapi.gen.JNIEnvVar
import com.epam.drill.jvmapi.gen.JVMTI_ENABLE
import com.epam.drill.jvmapi.gen.JVMTI_EVENT_CLASS_FILE_LOAD_HOOK
import com.epam.drill.jvmapi.gen.SetEventNotificationMode
import com.epam.drill.jvmapi.gen.jthread
import com.epam.drill.jvmapi.gen.jvmtiEnvVar
import com.epam.drill.logging.LoggingConfiguration
import com.epam.drill.test.agent.configuration.AgentConfig
import com.epam.drill.test.agent.instrument.StrategyManager
import com.epam.drill.test.agent.session.SessionController

private const val isHttpHookEnabled = false // based on args

@SharedImmutable
private val logger = KotlinLogging.logger("com.epam.drill.test.agent.jvmti.event.VmInitEvent")

@Suppress("UNUSED_PARAMETER")
fun vmInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    logger.debug { "Init event" }
    initRuntimeIfNeeded()
    val agentConfig = AgentConfig.config
    agentConfig.run {
        logger.trace { "Initializing StrategyManager" }
        StrategyManager.initialize(rawFrameworkPlugins, isManuallyControlled)
        logger.trace { "Configuring logging" }
        callObjectVoidMethod(LoggingConfiguration::class, LoggingConfiguration::readDefaultConfiguration.name)
        callObjectVoidMethodWithString(LoggingConfiguration::class, "setLoggingLevels", logLevel)
        callObjectVoidMethodWithString(LoggingConfiguration::class, LoggingConfiguration::setLoggingFilename, logFile)
        callObjectVoidMethodWithInt(LoggingConfiguration::class, LoggingConfiguration::setLogMessageLimit, logLimit)
    }
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)
    logger.trace { "Configuring HTTP hook (isHttpHookEnabled=$isHttpHookEnabled)" }
    if (isHttpHookEnabled) configureHooks()
}

private fun configureHooks() {
    logger.debug { "Drill interceptor is unavailable" }
}
