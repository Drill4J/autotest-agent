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
package com.epam.drill.test.agent.jvmti

import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlin.native.CName
import kotlin.native.concurrent.freeze
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.`value`
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import com.epam.drill.jvmapi.checkEx
import com.epam.drill.jvmapi.env
import com.epam.drill.jvmapi.gen.JVMTI_VERSION
import com.epam.drill.jvmapi.gen.JavaVMVar
import com.epam.drill.jvmapi.gen.jvmtiEnvVar
import com.epam.drill.jvmapi.gen.jvmtiError
import com.epam.drill.jvmapi.jvmti
import com.epam.drill.jvmapi.vmGlobal
import com.epam.drill.test.agent.Agent

@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: CPointer<JavaVMVar>, options: String, reservedPtr: Long): Int = memScoped {
    vmGlobal.value = vmPointer.freeze()
    val vm = vmPointer.pointed
    val jvmtiEnvPtr = alloc<CPointerVar<jvmtiEnvVar>>()
    vm.value!!.pointed.GetEnv!!(vm.ptr, jvmtiEnvPtr.ptr.reinterpret(), JVMTI_VERSION.convert())
    jvmti.value = jvmtiEnvPtr.value
    jvmtiEnvPtr.value.freeze()
    Agent.agentOnLoad(options)
}

@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Agent_OnUnload")
fun agentOnUnload(vmPointer: CPointer<JavaVMVar>) = Agent.agentOnUnload()

@Suppress("UNUSED")
@CName("checkEx")
fun checkEx(errCode: jvmtiError, funName: String) = checkEx(errCode, funName)

@Suppress("UNUSED")
@CName("currentEnvs")
fun currentEnvs() = env

@Suppress("UNUSED")
@CName("jvmtii")
fun jvmtii() = jvmti.value

@Suppress("UNUSED")
@CName("getJvm")
fun getJvm() = vmGlobal.value
