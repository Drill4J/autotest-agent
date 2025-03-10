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
package com.epam.drill.agent.test.instrument

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import com.epam.drill.agent.jvmapi.gen.CallObjectMethod
import com.epam.drill.agent.jvmapi.gen.NewStringUTF
import com.epam.drill.agent.jvmapi.gen.jobject
import com.epam.drill.agent.jvmapi.getObjectMethod
import com.epam.drill.agent.jvmapi.toByteArray
import com.epam.drill.agent.jvmapi.toJByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

actual object AgentClassTransformer {
    @OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
    actual fun transform(className: String, classBytes: ByteArray, loader: Any?, protectionDomain: Any?): ByteArray? =
        callAgentClassTransformerTransformMethod(
            AgentClassTransformer::class,
            AgentClassTransformer::transform,
            className,
            classBytes,
            loader,
            protectionDomain
        )
}

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
private fun callAgentClassTransformerTransformMethod(
    clazz: KClass<out Any>,
    method: KCallable<ByteArray?>,
    className: String,
    classBytes: ByteArray,
    loader: Any?,
    protectionDomain: Any?
) = getObjectMethod(clazz, method.name, "(Ljava/lang/String;[BLjava/lang/Object;Ljava/lang/Object;)[B").run {
    CallObjectMethod(
        this.first,
        this.second,
        NewStringUTF(className),
        toJByteArray(classBytes),
        loader as jobject?,
        protectionDomain as jobject?
    )?.let(::toByteArray)
}
