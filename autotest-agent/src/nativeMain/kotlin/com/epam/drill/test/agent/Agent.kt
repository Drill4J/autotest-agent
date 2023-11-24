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
package com.epam.drill.test.agent

import com.epam.drill.jvmapi.gen.AddCapabilities
import com.epam.drill.jvmapi.gen.AddToBootstrapClassLoaderSearch
import com.epam.drill.jvmapi.gen.JNI_OK
import com.epam.drill.jvmapi.gen.SetEventCallbacks
import com.epam.drill.jvmapi.gen.jvmtiCapabilities
import com.epam.drill.jvmapi.gen.jvmtiEventCallbacks
import com.epam.drill.logging.LoggingConfiguration
import com.epam.drill.test.agent.configuration.AgentConfig
import com.epam.drill.test.agent.configuration.AgentRawConfig
import com.epam.drill.test.agent.jvmti.enableJvmtiEventVmDeath
import com.epam.drill.test.agent.jvmti.enableJvmtiEventVmInit
import com.epam.drill.test.agent.jvmti.event.classFileLoadHook
import com.epam.drill.test.agent.jvmti.event.vmDeathEvent
import com.epam.drill.test.agent.jvmti.event.vmInitEvent
import com.epam.drill.test.agent.serialization.StringPropertyDecoder
import com.epam.drill.test.agent.session.SessionController
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlin.native.concurrent.freeze
import mu.KotlinLogging

object Agent {

    private val logger = KotlinLogging.logger("com.epam.drill.test.agent.Agent")

    fun agentOnLoad(options: String): Int = memScoped {
        try {
            val config = options.toAgentParams().freeze()
            setUnhandledExceptionHook({ thr: Throwable ->
                logger.error(thr) { "Unhandled event $thr" }
            }.freeze())
            val jvmtiCapabilities = alloc<jvmtiCapabilities>()
            jvmtiCapabilities.can_retransform_classes = 1.toUInt()
            jvmtiCapabilities.can_retransform_any_class = 1.toUInt()
            jvmtiCapabilities.can_maintain_original_method_order = 1.toUInt()
            AddCapabilities(jvmtiCapabilities.ptr)
            AddToBootstrapClassLoaderSearch("${config.drillInstallationDir}/drillRuntime.jar")
            callbackRegister()

            config.browserProxyAddress?.takeIf { "/" in it }?.let {
                logger.warn { "Expected format for a browser proxy is hostname.com:1234" }
            }

            if (config.devToolsProxyAddress.isNullOrBlank()) {
                logger.error { "UI coverage will be lost. Please specify devToolsProxyAddress" }
            }
            AgentConfig.updateConfig(config)
        } catch (ex: Throwable) {
            logger.error(ex) { "Can't load the agent. Reason:" }
        }
        return JNI_OK
    }

    fun agentOnUnload() {
    }

    private fun callbackRegister() = memScoped {
        val eventCallbacks = alloc<jvmtiEventCallbacks>()
        eventCallbacks.VMInit = staticCFunction(::vmInitEvent)
        eventCallbacks.VMDeath = staticCFunction(::vmDeathEvent)
        eventCallbacks.ClassFileLoadHook = staticCFunction(::classFileLoadHook)
        SetEventCallbacks(eventCallbacks.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        enableJvmtiEventVmInit()
        enableJvmtiEventVmDeath()
    }

}

private const val WRONG_PARAMS = "Agent parameters are not specified correctly."

private fun String?.toAgentParams() =
    AgentRawConfig.serializer().deserialize(StringPropertyDecoder(this.asParams())).also {
        println(it)
        if (it.agentId.isBlank() && it.groupId.isBlank()) {
            error(WRONG_PARAMS)
        }
        LoggingConfiguration.readDefaultConfiguration()
        LoggingConfiguration.setLoggingFilename(it.logFile)
        LoggingConfiguration.setLoggingLevels(it.logLevel)
        LoggingConfiguration.setLogMessageLimit(it.logLimit)
    }

private fun String?.asParams(): Map<String, String> =
    try {
        this?.split(",")?.filter(String::isNotEmpty)?.associate {
            val (key, value) = it.split("=")
            key to value
        } ?: emptyMap()
    } catch (parseException: Exception) {
        throw IllegalArgumentException(WRONG_PARAMS)
    }
