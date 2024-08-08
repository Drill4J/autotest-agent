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

import com.epam.drill.jvmapi.gen.*
import com.epam.drill.test.agent.instrument.*
import io.ktor.utils.io.bits.*
import kotlinx.cinterop.*
import mu.KotlinLogging

object ClassFileLoadHook {

    private val logger = KotlinLogging.logger("com.epam.drill.test.agent.instrumenting.ClassFileLoadHook")
    private const val DRILL_PACKAGE = "com/epam/drill"

    @OptIn(ExperimentalForeignApi::class)
    operator fun invoke(
        loader: jobject?,
        kClassName: CPointer<ByteVar>?,
        protectionDomain: jobject?,
        classDataLen: jint,
        classData: CPointer<UByteVar>?,
        newClassDataLen: CPointer<jintVar>?,
        newData: CPointer<CPointerVar<UByteVar>>?
    ) {
        initRuntimeIfNeeded()
        val className = kClassName?.toKString() ?: return
        if (notSuitableClass(loader, protectionDomain, className, classData)
            && !className.contains("Http") // raw hack for http(s) classes
        ) return
        if (classData == null || className.startsWith(DRILL_PACKAGE)) return
        val classBytes = ByteArray(classDataLen).apply {
            Memory.of(classData, classDataLen).loadByteArray(0, this)
        }
        val instrumentedBytes = AgentClassTransformer.transform(className, classBytes, loader, protectionDomain) ?: return
        val instrumentedSize = instrumentedBytes.size
        logger.trace { "Class $className has been transformed" }
        logger.trace { "Applying instrumenting (old: $classDataLen to new: $instrumentedSize)" }
        Allocate(instrumentedSize.toLong(), newData)
        val newBytes = newData!!.pointed.value!!
        instrumentedBytes.forEachIndexed { index, byte ->
            newBytes[index] = byte.toUByte()
        }
        newClassDataLen?.pointed?.value = instrumentedSize
        logger.debug { "Successfully instrumented class $className" }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun notSuitableClass(
        loader: jobject?,
        protectionDomain: jobject?,
        className: String?,
        classData: CPointer<UByteVar>?
    ): Boolean =
        loader == null || protectionDomain == null || className == null || classData == null

}
