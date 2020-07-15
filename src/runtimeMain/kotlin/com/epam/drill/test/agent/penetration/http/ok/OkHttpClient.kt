package com.epam.drill.test.agent.penetration.http.ok

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.penetration.*
import javassist.*

class OkHttpClient : Strategy() {

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.interfaces.any { it.name == "okhttp3.internal.http.HttpCodec" }
    }

    override fun instrument(ctClass: CtClass): ByteArray {
        val sendRequestHeader = kotlin.runCatching { ctClass.getDeclaredMethod("writeRequestHeaders")}
        sendRequestHeader.getOrNull()?.insertBefore(
            """
                if ($IF_CONDITION) {
                    $1 = $1.newBuilder()
                            .addHeader($TEST_NAME_CALC_LINE)
                            .addHeader($SESSION_ID_CALC_LINE)
                            .build();
                }
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}
