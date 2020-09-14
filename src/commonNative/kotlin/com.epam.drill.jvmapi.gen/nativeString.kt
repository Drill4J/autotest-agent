package kotlinx.cinterop

import com.epam.drill.jvmapi.env
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.jvmapi.jni
import kotlinx.cinterop.invoke
import kotlinx.cinterop.toKString

fun jobject.toKString(): String? {
    return jni.GetStringUTFChars!!(env, this, null)?.toKString()
}
