@file:Suppress("UNUSED_PARAMETER")

package com.epam.drill.test.agent.instrumenting

import com.epam.drill.jvmapi.gen.*
import com.epam.drill.test.agent.*
import io.ktor.utils.io.bits.*
import kotlinx.cinterop.*

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
    val classBytes = ByteArray(classDataLen).apply {
        Memory.of(classData!!, classDataLen).loadByteArray(0, this)
    }
    val instrumentedBytes = AgentClassTransformer.transform(className, classBytes) ?: return
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