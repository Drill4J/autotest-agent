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

    const val engineSegment = "[engine:testng]"
    private const val drillListener = "DrillTestNGTestListener"
    private const val packagePath = "org.testng"
    override val id: String
        get() = "testng"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "$packagePath.TestRunner"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        createTestListener(pool, classLoader, protectionDomain)
        ctClass.constructors.forEach { it.insertAfter("addTestListener(new $drillListener());") }
        return ctClass.toBytecode()
    }

    private fun createTestListener(
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ) {
        val testListener = pool.makeClass(drillListener)
        testListener.interfaces = arrayOf(pool.get("$packagePath.ITestListener"))
        testListener.addMethod(
            CtMethod.make(
                """
                private String getParamsString($packagePath.ITestResult result) {
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
            """.trimIndent(), testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestStart($packagePath.ITestResult result) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("${engineSegment}/[class:" + result.getInstanceName() + "]/[method:" + result.getName() + getParamsString(result) + "]");
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                       public void onTestSuccess($packagePath.ITestResult result) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("${engineSegment}/[class:" + result.getInstanceName() + "]/[method:" + result.getName() + getParamsString(result) + "]", "PASSED");
                       }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestFailure($packagePath.ITestResult result) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("${engineSegment}/[class:" + result.getInstanceName() + "]/[method:" + result.getName() + getParamsString(result) + "]", "FAILED");      
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestSkipped($packagePath.ITestResult result) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}("${engineSegment}/[class:" + result.getInstanceName() + "]/[method:" + result.getName() + getParamsString(result) + "]");     
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestFailedButWithinSuccessPercentage($packagePath.ITestResult result) {
                            return; 
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onStart($packagePath.ITestContext result) {
                            return; 
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onFinish($packagePath.ITestContext result) {
                            return;            
                        }
                    """.trimIndent(),
                testListener
            )
        )

        testListener.toClass(classLoader, protectionDomain)
    }
}
