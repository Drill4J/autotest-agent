package com.epam.drill.test.agent

import com.epam.drill.test.agent.actions.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*


@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Java_com_epam_drill_test_agent_ThreadStorage_memorizeTestNameNative")
fun memorizeTestName(env: CPointer<JNIEnvVar>?, thisObj: jobject, inJNIStr: jstring) {
    val testNameFromJava: String =
        env?.pointed?.pointed?.GetStringUTFChars?.invoke(env, inJNIStr, null)?.toKString() ?: ""
    SessionController.testName.value = testNameFromJava
}

@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Java_com_epam_drill_test_agent_ThreadStorage_sessionId")
fun sessionId(env: CPointer<JNIEnvVar>?, thisObj: jobject): jstring? {
    return NewStringUTF(SessionController.sessionId.value)
}


@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Java_com_epam_drill_test_agent_ThreadStorage_proxyUrl")
fun proxyUrl(env: CPointer<JNIEnvVar>?, thisObj: jobject): jstring? {
    return NewStringUTF(SessionController.agentConfig.value.browserProxyAddress)
}

