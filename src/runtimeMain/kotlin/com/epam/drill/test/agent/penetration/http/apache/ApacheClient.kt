package com.epam.drill.test.agent.penetration.http.apache

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.penetration.*
import javassist.*

class ApacheClient : Strategy() {

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.interfaces.any { "org.apache.http.HttpClientConnection" == it.name }
    }

    override fun instrument(ctClass: CtClass): ByteArray {
        val sendRequestHeader = kotlin.runCatching { ctClass.getDeclaredMethod("sendRequestHeader") }

        sendRequestHeader.getOrNull()?.insertBefore(
            """
                    if($IF_CONDITION) {
                        ${'$'}1.setHeader($TEST_NAME_CALC_LINE);
                        ${'$'}1.setHeader($SESSION_ID_CALC_LINE);
                    }
                """
        )
        return ctClass.toBytecode()
    }
}
