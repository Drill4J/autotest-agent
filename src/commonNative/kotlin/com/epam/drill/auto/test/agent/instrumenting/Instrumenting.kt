@file:Suppress("UNUSED_PARAMETER")

package com.epam.drill.auto.test.agent.instrumenting

import com.epam.drill.auto.test.agent.*
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
    val className = kClassName?.toKString()
    if (notSuitableClass(loader, protection_domain, className, classData)) return

    val instrumentedBytes = transform(classData, className!!) ?: return
    val instrumentedSize = instrumentedBytes.size
    mainLogger.debug { "Class '$className' was transformed" }
    mainLogger.debug { "Applying instrumenting (old: $classDataLen to new: $instrumentedSize)" }
    Allocate(instrumentedSize.toLong(), newData)
    val newBytes = newData!!.pointed.value!!
    instrumentedBytes.forEachIndexed { index, byte ->
        newBytes[index] = byte.toUByte()
    }
    newClassDataLen!!.pointed.value = instrumentedSize
    mainLogger.info { "Successfully instrumented class $className" }
}

private fun notSuitableClass(
    loader: jobject?,
    protection_domain: jobject?,
    className: String?,
    classData: CPointer<UByteVar>?
): Boolean =
    loader == null || protection_domain == null || className == null || classData == null

val transformerClass: jclass
    get() = FindClass("com/epam/drill/auto/test/agent/AgentClassTransformer")
        ?: error("No AgentClassTransformer class!")

fun initializeStrategyManager(rawFrameworkPlugins: String) {
    val managerClass = FindClass("com/epam/drill/auto/test/agent/penetration/StrategyManager")
        ?: error("No StrategyManager class!")
    val initialize: jmethodID? = GetStaticMethodID(managerClass, "initialize", "(Ljava/lang/String;)V")
    CallStaticObjectMethod(managerClass, initialize, NewStringUTF(rawFrameworkPlugins))
}

fun transform(classBytes: CPointer<UByteVar>?, className: String): ByteArray? {
    val transform: jmethodID? = GetStaticMethodID(transformerClass, "transform", "(Ljava/lang/String;[B)[B")
    return CallStaticObjectMethod(transformerClass, transform, NewStringUTF(className), classBytes).toByteArray()
}

fun jobject?.toByteArray(): ByteArray? = this?.run {
    val size = GetArrayLength(this)
    val getByteArrayElements: CPointer<ByteVarOf<jbyte>>? = GetByteArrayElements(this, null)
    return@run getByteArrayElements?.readBytes(size)
}
