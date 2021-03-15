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
package com.epam.drill.test.agent.instrumentation.testing.testng

import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import com.epam.drill.test.agent.TestListener
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import java.security.ProtectionDomain

@Suppress("unused")
object TestNGStrategy : AbstractTestStrategy() {

    override val id: String
        get() = "testng"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "org.testng.TestListenerAdapter"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        ctClass.addMethod(
            CtMethod.make(
                """
            private static String getParamsString(org.testng.ITestResult result) {
                Object[] parameters = result.getParameters();
                String paramString = "(";
                for(int i = 0; i < parameters.length; i++){ 
                    String parameterClassName = parameters[i].getClass().getSimpleName();
                    if(i != 0) {
                        paramString += ",";
                    }
                    paramString += parameterClassName;
                }
                paramString += ")";
                if(result.getParameters().length != 0){
                    paramString += "[" + result.getMethod().getParameterInvocationCount() + "]";
                }
                return paramString;
            }
        """.trimIndent(), ctClass
            )
        )
        sequenceOf(
            ctClass.getDeclaredMethod("onTestSuccess") to "PASSED",
            ctClass.getDeclaredMethod("onTestFailure") to "FAILED"
        ).forEach { (method, status) ->
            method.insertAfter(
                """
                   ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("[engine:testng]/[class:"+$1.getInstanceName()+"]/[method:"+$1.getName()+getParamsString($1)+"]", "$status");
            """.trimIndent()
            )
        }

        ctClass.getDeclaredMethod("onTestSkipped").insertAfter(
            """
                   ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}("[engine:testng]/[class:"+$1.getInstanceName()+"]/[method:"+$1.getName()+getParamsString($1)+"]");
            """.trimIndent()
        )

        ctClass.getDeclaredMethod("onTestStart").insertAfter(
            """
            ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("[engine:testng]/[class:"+$1.getInstanceName()+"]/[method:"+$1.getName()+getParamsString($1)+"]");
        """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}
