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
import java.lang.reflect.*
import java.security.ProtectionDomain

abstract class TestNGStrategy : AbstractTestStrategy() {
    companion object {
        const val engineSegment = "[engine:testng]"
        const val TestNGMethod = "org.testng.internal.TestNGMethod"
        const val ITestResult = "org.testng.ITestResult"

        private const val DrillTestNGTestListner = "DrillTestNGTestListener"
        private const val ITestContext = "org.testng.ITestContext"
    }

    abstract val versionRegex: Regex
    override val id: String
        get() = "testng"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "org.testng.TestRunner" && "${ctClass.url}".contains(versionRegex)
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        createTestListener(pool, classLoader, protectionDomain)
        ctClass.constructors.forEach { it.insertAfter("addTestListener(new $DrillTestNGTestListner());") }
        getIgnoredTests(ctClass, pool)
        return ctClass.toBytecode()
    }

    private fun createTestListener(
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ) {
        val testListener = pool.makeClass(DrillTestNGTestListner)
        testListener.interfaces = arrayOf(pool.get("org.testng.ITestListener"))
        testListener.addMethod(
            CtMethod.make(getParamsString(), testListener)
        )
        testListener.addMethod(
            CtMethod.make(getFactoryParams(), testListener)
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestStart($ITestResult result) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("$engineSegment/[class:" + result.getInstanceName() + getFactoryParams(result) + "]/[method:" + result.getName() + getParamsString(result) + "]");
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                       public void onTestSuccess($ITestResult result) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("$engineSegment/[class:" + result.getInstanceName() + getFactoryParams(result) + "]/[method:" + result.getName() + getParamsString(result) + "]", "PASSED");
                       }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestFailure($ITestResult result) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("$engineSegment/[class:" + result.getInstanceName() + getFactoryParams(result) + "]/[method:" + result.getName() + getParamsString(result) + "]", "FAILED");      
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestSkipped($ITestResult result) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}("$engineSegment/[class:" + result.getInstanceName() + getFactoryParams(result) + "]/[method:" + result.getName() + getParamsString(result) + "]");     
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestFailedButWithinSuccessPercentage($ITestResult result) {
                            return; 
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onStart($ITestContext result) {
                            return; 
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onFinish($ITestContext result) {
                            return;            
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.toClass(classLoader, protectionDomain)
    }

    private fun getParamsString(): String = """
        private String getParamsString($ITestResult result) {
            Object[] parameters = result.getParameters();
            String paramString = ${this::class.java.name}.INSTANCE.${this::paramTypes.name}(parameters);
            if (result.getParameters().length != 0){
                paramString += "[" + result.getMethod().getParameterInvocationCount() + "]";
            }
            return paramString;
        }
    """.trimIndent()

    abstract fun getFactoryParams(): String
    open fun getIgnoredTests(ctClass: CtClass, pool: ClassPool) {}

    fun paramTypes(objects: Array<Any>): String = objects.joinToString(",", "(", ")") {
        when (it) {
            is Field -> it.type.simpleName
            else -> it.javaClass.simpleName
        }
    }
}
