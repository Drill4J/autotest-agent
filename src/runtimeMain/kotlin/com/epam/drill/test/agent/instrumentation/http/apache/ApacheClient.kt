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
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray {
        val sendRequestHeader = kotlin.runCatching {
            ctClass.getDeclaredMethod("sendRequestHeader")
        }

        sendRequestHeader.getOrNull()?.insertBefore(
            """
                    if($IF_CONDITION) {
                        ${Log::class.java.name}.INSTANCE.${Log::injectHeaderLog.name}($TEST_NAME_VALUE_CALC_LINE, $SESSION_ID_VALUE_CALC_LINE);
                        try{
                            $1.setHeader($TEST_NAME_CALC_LINE);
                            $1.setHeader($SESSION_ID_CALC_LINE);
                        }catch( Exception e){
                            e.printStackTrace();
                        }
                    }
                """
        )
        return ctClass.toBytecode()
    }
}
