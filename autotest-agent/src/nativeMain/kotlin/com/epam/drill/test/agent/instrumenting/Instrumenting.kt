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
@file:Suppress("UNUSED_PARAMETER")

package com.epam.drill.test.agent.instrumenting

import com.epam.drill.jvmapi.gen.*
import com.epam.drill.test.agent.*
import io.ktor.utils.io.bits.*
import kotlinx.cinterop.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger("com.epam.drill.test.agent.instrumenting.Instrumenting")

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
    val instrumentedBytes = AgentClassTransformer.transform(className, classBytes, loader, protection_domain) ?: return
    val instrumentedSize = instrumentedBytes.size
    logger.debug { "Class '$className' was transformed" }
    logger.debug { "Applying instrumenting (old: $classDataLen to new: $instrumentedSize)" }
    Allocate(instrumentedSize.toLong(), newData)
    val newBytes = newData!!.pointed.value!!
    instrumentedBytes.forEachIndexed { index, byte ->
        newBytes[index] = byte.toUByte()
    }
    newClassDataLen?.pointed?.value = instrumentedSize
    logger.info { "Successfully instrumented class $className" }
}

private fun notSuitableClass(
    loader: jobject?,
    protection_domain: jobject?,
    className: String?,
    classData: CPointer<UByteVar>?
): Boolean =
    loader == null || protection_domain == null || className == null || classData == null
