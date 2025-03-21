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
package com.epam.drill.agent.test.jvmti

import com.epam.drill.agent.jvmapi.gen.JNIEnvVar
import com.epam.drill.agent.jvmapi.gen.jclass
import com.epam.drill.agent.jvmapi.gen.jint
import com.epam.drill.agent.jvmapi.gen.jintVar
import com.epam.drill.agent.jvmapi.gen.jobject
import com.epam.drill.agent.jvmapi.gen.jthread
import com.epam.drill.agent.jvmapi.gen.jvmtiEnvVar
import com.epam.drill.agent.test.Agent
import kotlinx.cinterop.*

@Suppress("unused_parameter")
@OptIn(ExperimentalForeignApi::class)
fun vmInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) = Agent.agentOnVmInit()

@Suppress("unused_parameter")
@OptIn(ExperimentalForeignApi::class)
fun vmDeathEvent(jvmtiEnv: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?) = Agent.agentOnVmDeath()

@Suppress("unused_parameter")
@OptIn(ExperimentalForeignApi::class)
fun classFileLoadHook(
    jvmtiEnv: CPointer<jvmtiEnvVar>?,
    jniEnv: CPointer<JNIEnvVar>?,
    classBeingRedefined: jclass?,
    loader: jobject?,
    clsName: CPointer<ByteVar>?,
    protectionDomain: jobject?,
    classDataLen: jint,
    classData: CPointer<UByteVar>?,
    newClassDataLen: CPointer<jintVar>?,
    newData: CPointer<CPointerVar<UByteVar>>?,
) = ClassFileLoadHook.invoke(
    loader,
    clsName,
    protectionDomain,
    classDataLen,
    classData,
    newClassDataLen,
    newData
)
