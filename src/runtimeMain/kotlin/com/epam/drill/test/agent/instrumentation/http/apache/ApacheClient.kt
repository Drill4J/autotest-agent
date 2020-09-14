package com.epam.drill.test.agent.instrumentation.http.apache

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.*
import com.epam.drill.test.agent.instrumentation.http.Log
import javassist.*
import java.security.ProtectionDomain

class ApacheClient : Strategy() {

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.interfaces.any { "org.apache.http.HttpClientConnection" == it.name }
    }

    override fun instrument(
        ctClass: CtClass,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray {
        val sendRequestHeader = kotlin.runCatching { ctClass.getDeclaredMethod("sendRequestHeader") }

        sendRequestHeader.getOrNull()?.insertBefore(
            """
                    if($IF_CONDITION) {
                        ${Log::class.java.name}.INSTANCE.${Log::injectHeaderLog.name}($TEST_NAME_VALUE_CALC_LINE, $SESSION_ID_VALUE_CALC_LINE);
                        ${'$'}1.setHeader($TEST_NAME_CALC_LINE);
                        ${'$'}1.setHeader($SESSION_ID_CALC_LINE);
                    }
                """
        )
        return ctClass.toBytecode()
    }
}
