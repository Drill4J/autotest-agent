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

import com.epam.drill.test.agent.jvmti.classFileLoadHook
import com.epam.drill.test.agent.jvmti.vmInitEvent
import com.epam.drill.test.agent.jvmti.vmDeathEvent
import com.epam.drill.test.agent.session.SessionController
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlin.native.concurrent.freeze
import com.epam.drill.agent.configuration.DefaultParameterDefinitions.INSTALLATION_DIR
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.test.agent.configuration.AgentLoggingConfiguration
import com.epam.drill.test.agent.configuration.Configuration
import com.epam.drill.test.agent.configuration.ParameterDefinitions.FRAMEWORK_PLUGINS
import com.epam.drill.test.agent.instrument.StrategyManager
import mu.KotlinLogging
import platform.posix.getpid
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

object Agent {

    private val logger = KotlinLogging.logger("com.epam.drill.test.agent.Agent")

    private val logo = """
          ____    ____                 _       _          _  _                _      
         |  _"\U |  _"\ u     ___     |"|     |"|        | ||"|            U |"| u   
        /| | | |\| |_) |/    |_"_|  U | | u U | | u      | || |_          _ \| |/    
        U| |_| |\|  _ <       | |    \| |/__ \| |/__     |__   _|        | |_| |_,-. 
         |____/ u|_| \_\    U/| |\u   |_____| |_____|      /|_|\          \___/-(_/  
          |||_   //   \\_.-,_|___|_,-.//  \\  //  \\      u_|||_u          _//       
         (__)_) (__)  (__)\_)-' '-(_/(_")("_)(_")("_)     (__)__)         (__)  
         Autotest Agent (v${agentVersion})
        """.trimIndent()

    @OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
    fun agentOnLoad(options: String): Int = memScoped {
        println(logo)

        AgentLoggingConfiguration.defaultNativeLoggingConfiguration()
        Configuration.initializeNative(options)
        AgentLoggingConfiguration.updateNativeLoggingConfiguration()

        addCapabilities()
        setEventCallbacks()
        setUnhandledExceptionHook({ thr: Throwable -> logger.error(thr) { "Unhandled event $thr" } }.freeze())
        AddToBootstrapClassLoaderSearch("${Configuration.parameters[INSTALLATION_DIR]}/drill-runtime.jar")

        logger.info { "agentOnLoad: Autotest agent has been loaded. Pid is: " + getpid() }

        return JNI_OK
    }

    fun agentOnUnload() {
        logger.info { "agentOnUnload:  Autotest agent has been unloaded." }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun agentOnVmInit() {
        logger.debug { "Init event" }
        initRuntimeIfNeeded()
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)

        AgentLoggingConfiguration.defaultJvmLoggingConfiguration()
        AgentLoggingConfiguration.updateJvmLoggingConfiguration()
        Configuration.initializeJvm()

        SessionController.startSession()
        logger.trace { "Initializing StrategyManager..." }
        StrategyManager.initialize(
            Configuration.parameters[FRAMEWORK_PLUGINS].joinToString(";")
        )
    }

    fun agentOnVmDeath() {
        logger.debug { "Death Event" }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun addCapabilities() = memScoped {
        val jvmtiCapabilities = alloc<jvmtiCapabilities>()
        jvmtiCapabilities.can_retransform_classes = 1.toUInt()
        jvmtiCapabilities.can_retransform_any_class = 1.toUInt()
        jvmtiCapabilities.can_maintain_original_method_order = 1.toUInt()
        AddCapabilities(jvmtiCapabilities.ptr)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setEventCallbacks() = memScoped {
        val eventCallbacks = alloc<jvmtiEventCallbacks>()
        eventCallbacks.VMInit = staticCFunction(::vmInitEvent)
        eventCallbacks.VMDeath = staticCFunction(::vmDeathEvent)
        eventCallbacks.ClassFileLoadHook = staticCFunction(::classFileLoadHook)
        SetEventCallbacks(eventCallbacks.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null)
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, null)
    }

}
