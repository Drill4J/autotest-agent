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

import com.epam.drill.agent.test.instrument.StrategyManager
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlinx.cinterop.toByte
import com.epam.drill.agent.jvmapi.gen.CallVoidMethod
import com.epam.drill.agent.jvmapi.gen.NewStringUTF
import com.epam.drill.agent.jvmapi.getObjectMethod
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

actual object StrategyManager {
    actual fun initialize(rawFrameworkPlugins: String): Unit =
        callObjectVoidMethodWithString(
            StrategyManager::class,
            StrategyManager::initialize,
            rawFrameworkPlugins
        )
}
@OptIn(ExperimentalForeignApi::class)
private fun callObjectVoidMethodWithString(
    clazz: KClass<out Any>,
    method: String,
    string: String?
) = getObjectMethod(clazz, method, "(Ljava/lang/String;)V").run {
    CallVoidMethod(this.first, this.second, string?.let(::NewStringUTF))
}

private fun callObjectVoidMethodWithString(
    clazz: KClass<out Any>,
    method: KCallable<Any?>,
    string: String?
) = callObjectVoidMethodWithString(clazz, method.name, string)
