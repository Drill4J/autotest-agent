package com.epam.drill.test.agent.jvmti

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.UByteVar
import com.epam.drill.jvmapi.gen.JNIEnvVar
import com.epam.drill.jvmapi.gen.jclass
import com.epam.drill.jvmapi.gen.jint
import com.epam.drill.jvmapi.gen.jintVar
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.jvmapi.gen.jthread
import com.epam.drill.jvmapi.gen.jvmtiEnvVar
import com.epam.drill.test.agent.Agent
import com.epam.drill.test.agent.jvmti.event.ClassFileLoadHook

@Suppress("unused_parameter")
fun vmInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) = Agent.agentOnVmInit()

@Suppress("unused_parameter")
fun vmDeathEvent(jvmtiEnv: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?) = Agent.agentOnVmDeath()

@Suppress("unused_parameter")
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
