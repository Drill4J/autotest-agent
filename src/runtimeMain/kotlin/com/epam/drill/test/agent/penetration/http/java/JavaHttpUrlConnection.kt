package com.epam.drill.test.agent.penetration.http.java

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.penetration.*
import javassist.*

class JavaHttpUrlConnection : Strategy() {

    override fun permit(ctClass: CtClass): Boolean {
        val parentClassName = ctClass.superclass.name
        return parentClassName == "java.net.HttpURLConnection" ||
                parentClassName == "javax.net.ssl.HttpsURLConnection"
    }

    override fun instrument(ctClass: CtClass): ByteArray {
        val sendRequestHeader = kotlin.runCatching { ctClass.constructors }
        sendRequestHeader.getOrNull()?.forEach {
            it.insertAfter(
                """
                        if ($IF_CONDITION) {
                            try {
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
