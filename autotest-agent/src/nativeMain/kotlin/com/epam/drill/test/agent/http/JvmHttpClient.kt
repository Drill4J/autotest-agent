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
package com.epam.drill.test.agent.http

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlinx.cinterop.toKString
import com.epam.drill.jvmapi.gen.CallObjectMethod
import com.epam.drill.jvmapi.gen.GetStringUTFChars
import com.epam.drill.jvmapi.gen.NewStringUTF
import com.epam.drill.jvmapi.getObjectMethod

actual object JvmHttpClient {
    actual fun httpCall(endpoint: String, request: String): String =
        callObjectStringMethodWithStrings(JvmHttpClient::class, JvmHttpClient::httpCall, endpoint, request)!!
}

private fun callObjectStringMethodWithStrings(
    clazz: KClass<out Any>,
    method: String,
    string1: String?,
    string2: String?
) = getObjectMethod(clazz, method, "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;").run {
    CallObjectMethod(this.first, this.second, string1?.let(::NewStringUTF), string2?.let(::NewStringUTF))?.let {
        GetStringUTFChars(it, null)?.toKString()
    }
}

private fun callObjectStringMethodWithStrings(
    clazz: KClass<out Any>,
    method: KCallable<Any?>,
    string1: String?,
    string2: String?
) = callObjectStringMethodWithStrings(clazz, method.name, string1, string2)
