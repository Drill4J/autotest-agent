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
package com.epam.drill

import com.epam.drill.jvmapi.callNativeVoidMethodWithString
import com.epam.drill.jvmapi.ex
import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.jvmapi.gen.jstring
import com.epam.drill.jvmapi.withJString
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret

@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Java_com_epam_drill_SessionProvider_startSession")
fun startSession(
    env: JNIEnv,
    thiz: jobject,
    sessionId: jstring,
    testType: jstring,
    isRealtime: UByte,
    testName: jstring?,
    isGlobal: UByte
) = memScoped {
    withJString {
        ex = env.getPointer(this@memScoped).reinterpret()
        SessionProvider.startSession(
            sessionId.toKString(),
            testType.toKString(),
            isRealtime == 1.toUByte(),
            testName?.toKString(),
            isGlobal == 1.toUByte()
        )
    }
}

@Suppress("UNUSED")
@CName("Java_com_epam_drill_SessionProvider_stopSession")
fun stopSession(env: JNIEnv, thiz: jobject, sessionId: jstring?) =
    callNativeVoidMethodWithString(env, thiz, SessionProvider::stopSession, sessionId)


@Suppress("UNUSED")
@CName("Java_com_epam_drill_SessionProvider_setTestName")
fun setTestName(env: JNIEnv, thiz: jobject, testName: jstring?) =
    callNativeVoidMethodWithString(env, thiz, SessionProvider::setTestName, testName)
