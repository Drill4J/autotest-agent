@file:Suppress("UNUSED_PARAMETER")

package com.epam.drill.test.agent.instrumenting

import com.epam.drill.test.agent.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*

@CName("jvmtiEventClassFileLoadHookEvent")
fun classFileLoadHookEvent(
    jvmtiEnv: CPointer<jvmtiEnvVar>?,
    jniEnv: CPointer<JNIEnvVar>?,
    classBeingRedefined: jclass?,
    loader: jobject?,
    kClassName: CPointer<ByteVar>?,
    protection_domain: jobject?,
    classDataLen: jint,
    classData: CPointer<UByteVar>?,
    newClassDataLen: CPointer<jintVar>?,
    newData: CPointer<CPointerVar<UByteVar>>?
) {
    initRuntimeIfNeeded()
    val className = kClassName?.toKString() ?: return
    if (notSuitableClass(loader, protection_domain, className, classData)
        && !className.contains("Http") // raw hack for http(s) classes
    ) return
    val instrumentedBytes = transform(classData, className, classDataLen) ?: return
    val instrumentedSize = instrumentedBytes.size
    mainLogger.debug { "Class '$className' was transformed" }
    mainLogger.debug { "Applying instrumenting (old: $classDataLen to new: $instrumentedSize)" }
    Allocate(instrumentedSize.toLong(), newData)
    val newBytes = newData!!.pointed.value!!
    instrumentedBytes.forEachIndexed { index, byte ->
        newBytes[index] = byte.toUByte()
    }
    newClassDataLen?.pointed?.value = instrumentedSize
    mainLogger.info { "Successfully instrumented class $className" }
}

private fun notSuitableClass(
    loader: jobject?,
    protection_domain: jobject?,
    className: String?,
    classData: CPointer<UByteVar>?
): Boolean =
    loader == null || protection_domain == null || className == null || classData == null

fun initializeStrategyManager(rawFrameworkPlugins: String) {
    val (mangerClass, managerObject) = instance("com/epam/drill/test/agent/penetration/StrategyManager")
    val initialize: jmethodID? = GetMethodID(mangerClass, "initialize", "(Ljava/lang/String;)V")
    CallObjectMethod(managerObject, initialize, NewStringUTF(rawFrameworkPlugins))
}

fun transform(classBytes: CPointer<UByteVar>?, className: String, classDataLen: jint): ByteArray? {
    val (agentClass, agentObject) = instance("com/epam/drill/test/agent/AgentClassTransformer")
    val transform: jmethodID? = GetMethodID(agentClass, "transform", "(Ljava/lang/String;[B)[B")
    val classBytesInJBytesArray: jbyteArray = NewByteArray(classDataLen)!!
    val readBytes = classBytes!!.readBytes(classDataLen)
    SetByteArrayRegion(classBytesInJBytesArray, 0, classDataLen, readBytes.refTo(0))
    return CallObjectMethod(
        agentObject,
        transform,
        NewStringUTF(className),
        classBytesInJBytesArray
    ).toByteArray()
}

fun jobject?.toByteArray(): ByteArray? = this?.run {
    val size = GetArrayLength(this)
    val getByteArrayElements: CPointer<ByteVarOf<jbyte>>? = GetByteArrayElements(this, null)
    return@run getByteArrayElements?.readBytes(size)
}