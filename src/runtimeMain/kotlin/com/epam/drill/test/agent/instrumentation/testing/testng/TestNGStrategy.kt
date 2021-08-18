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

    private fun CtClass.isTestRunner() = name == "$packagePath.TestRunner"
    private fun CtClass.isSuiteRunner() = interfaces.any { it.name == "$packagePath.ISuite" }
    private fun CtClass.isAnnotationHelper() = name == "$packagePath.internal.annotations.AnnotationHelper"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.isTestRunner() || ctClass.isSuiteRunner() || ctClass.isAnnotationHelper()
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        if (ctClass.isTestRunner()) {
            createTestListener(pool, classLoader, protectionDomain)
            ctClass.constructors.forEach { it.insertAfter("addTestListener(new $drillListener());") }
        }
        if (ctClass.isSuiteRunner()) {
            ctClass.disabledTestsSupport()
        }
        // Only for testng 7.4.0
        if (ctClass.isAnnotationHelper() && pool.getOrNull("$packagePath.annotations.IIgnoreAnnotation") != null) {
            ctClass.ignoredTestSupport()
        }
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
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("$engineSegment/[class:" + result.getInstanceName() + "]/[method:" + result.getName() + getParamsString(result) + "]");
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                       public void onTestSuccess($packagePath.ITestResult result) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("$engineSegment/[class:" + result.getInstanceName() + "]/[method:" + result.getName() + getParamsString(result) + "]", "PASSED");
                       }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestFailure($packagePath.ITestResult result) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("$engineSegment/[class:" + result.getInstanceName() + "]/[method:" + result.getName() + getParamsString(result) + "]", "FAILED");      
                        }
                    """.trimIndent(),
                testListener
            )
        )
        testListener.addMethod(
            CtMethod.make(
                """
                        public void onTestSkipped($packagePath.ITestResult result) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}("$engineSegment/[class:" + result.getInstanceName() + "]/[method:" + result.getName() + getParamsString(result) + "]");     
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

    /**
     * Support for tests, disabled by changing flag in @Test(enabled = false) annotation
     * When engine version is lower then 7.4.0 also support @Ignore annotation
     */
    private fun CtClass.disabledTestsSupport() = getDeclaredMethod("run").insertAfter(
            """
                java.util.Iterator disabledTests = getExcludedMethods().iterator();
                while(disabledTests.hasNext()) {
                    $packagePath.ITestNGMethod test = ($packagePath.ITestNGMethod) disabledTests.next();
                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}("$engineSegment/[class:" + test.getTestClass().getName() + "]/[method:" + test.getMethodName() + "]");     
                }
            """.trimIndent()
    )

    /**
     * Support 7.4.0 testng @Ignore annotation
     */
    private fun CtClass.ignoredTestSupport() = getMethod(
        "isAnnotationPresent",
        "(Lorg/testng/internal/annotations/IAnnotationFinder;Ljava/lang/reflect/Method;Ljava/lang/Class;)Z"
    ).insertAfter(
        """ 
            if ($3 == $packagePath.annotations.IIgnoreAnnotation.class && ${'$'}_) {
                ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}("$engineSegment/[class:" + $2.getDeclaringClass().getName() + "]/[method:" + $2.getName() + "]");
            }
        """.trimIndent())
}


