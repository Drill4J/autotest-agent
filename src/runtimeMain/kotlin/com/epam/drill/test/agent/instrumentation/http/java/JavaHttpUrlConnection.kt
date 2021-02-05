/**
 * Copyright 2020 EPAM Systems
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
        pool: ClassPool,
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
