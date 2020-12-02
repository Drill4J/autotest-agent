package com.epam.drill.test.agent.instrumentation.http.java

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.*
import com.epam.drill.test.agent.instrumentation.http.Log
import javassist.*
import java.security.ProtectionDomain

class JavaHttpUrlConnection : Strategy() {

    override fun permit(ctClass: CtClass): Boolean {
        val parentClassName = ctClass.superclass.name
        return parentClassName == "java.net.HttpURLConnection" ||
                parentClassName == "javax.net.ssl.HttpsURLConnection"
    }

    override fun instrument(
        ctClass: CtClass,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray {
        val sendRequestHeader = kotlin.runCatching { ctClass.constructors }.onFailure {
            logger.error(it) { "Error while instrumenting the class ${ctClass.name}" }
        }
        sendRequestHeader.getOrNull()?.forEach {
            it.insertAfter(
                """
                        if ($IF_CONDITION) {
                            try {
                                ${Log::class.java.name}.INSTANCE.${Log::injectHeaderLog.name}($TEST_NAME_VALUE_CALC_LINE, $SESSION_ID_VALUE_CALC_LINE);
                                this.setRequestProperty($TEST_NAME_CALC_LINE);
                                this.setRequestProperty($SESSION_ID_CALC_LINE);
                            } catch (Exception e) {};
                        }
                    """
            )
        }
        return ctClass.toBytecode()
    }
}
