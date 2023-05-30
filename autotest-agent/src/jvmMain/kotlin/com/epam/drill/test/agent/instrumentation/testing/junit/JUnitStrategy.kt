/**
 * Copyright 2020 - 2022 EPAM Systems
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
package com.epam.drill.test.agent.instrumentation.testing.junit

import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.*
import javassist.*
import org.objectweb.asm.*
import java.security.*

@Suppress("unused")
object JUnitStrategy : AbstractTestStrategy() {
    const val engineSegment = "junit"

    override val id: String
        get() = "junit"

    override fun permit(classReader: ClassReader): Boolean {
        return classReader.className == "org/junit/runner/notification/RunNotifier"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        val cc: CtClass = pool.makeClass("MyList")
        cc.superclass = pool.get("org.junit.runner.notification.RunListener")
        cc.addField(CtField.make("org.junit.runner.notification.RunListener mainRunner = null;", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                    public MyList(org.junit.runner.notification.RunListener mainRunner) {
                        this.mainRunner = mainRunner;
                    }
                        """.trimMargin(), cc
            )
        )
        val dp = """description"""
        cc.addMethod(
            CtMethod.make(
                """
                    public void testRunStarted(org.junit.runner.Description $dp) throws Exception {
                        this.mainRunner.testRunStarted($dp);
                    }
                        """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                    public void testStarted(org.junit.runner.Description $dp) throws Exception {
                        this.mainRunner.testStarted($dp);
                        ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("$engineSegment", $dp.getClassName(), $dp.getMethodName());
                    }
                        """.trimIndent(),
                cc
            )
        )


        cc.addMethod(
            CtMethod.make(
                """
                    public void testFinished(org.junit.runner.Description $dp) throws Exception {
                        this.mainRunner.testFinished(description);
                        ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("$engineSegment", $dp.getClassName(), $dp.getMethodName(), "${TestResult.PASSED.name}");
                    }
                        """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                    public void testRunFinished(org.junit.runner.Result result) throws Exception {
                        this.mainRunner.testRunFinished(result);
                    }
                        """.trimIndent(),
                cc
            )
        )


        val failureParamName = """failure"""
        val desct = """$failureParamName.getDescription()"""
        cc.addMethod(
            CtMethod.make(
                """
                    public void testFailure(org.junit.runner.notification.Failure $failureParamName) throws Exception {
                        this.mainRunner.testFailure($failureParamName);
                        ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("$engineSegment", $desct.getClassName(), $desct.getMethodName(), "${TestResult.FAILED.name}");
                    }
                        """.trimIndent(),
                cc
            )
        )


        cc.addMethod(
            CtMethod.make(
                """
                    public void testAssumptionFailure(org.junit.runner.notification.Failure $failureParamName) {
                        this.mainRunner.testAssumptionFailure(failure);
                    }
                        """.trimIndent(),
                cc
            )
        )



        cc.addMethod(
            CtMethod.make(
                """
                    public void testIgnored(org.junit.runner.Description $dp) throws Exception {
                        this.mainRunner.testIgnored($dp);
                        ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}("$engineSegment", $dp.getClassName(), $dp.getMethodName());      
                    }
                        """.trimIndent(),
                cc
            )
        )

        cc.toClass(classLoader, protectionDomain)
        ctClass.getDeclaredMethod("addListener").insertBefore(
            """
                $1= new MyList($1);
            """.trimIndent()
        )
        ctClass.getDeclaredMethod("addFirstListener").insertBefore(
            """
                $1= new MyList($1);
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}
