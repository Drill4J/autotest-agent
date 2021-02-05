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
package com.epam.drill.test.agent.instrumentation.testing.junit

import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import com.epam.drill.test.agent.TestListener
import javassist.*
import java.security.ProtectionDomain

@Suppress("unused")
object JUnit5Strategy : AbstractTestStrategy() {
    override val id: String
        get() = "junit"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "org.junit.platform.engine.support.hierarchical.NodeTestTaskContext"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        val cc: CtClass = pool.makeClass("MyList")
        cc.interfaces = arrayOf(pool.get("org.junit.platform.engine.EngineExecutionListener"))
        cc.addField(CtField.make("org.junit.platform.engine.EngineExecutionListener mainRunner = null;", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                            public MyList(org.junit.platform.engine.EngineExecutionListener mainRunner) { 
                               this.mainRunner = mainRunner;
                            }
                        """.trimMargin(), cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                            public void dynamicTestRegistered(org.junit.platform.engine.TestDescriptor testDescriptor) {
                                mainRunner.dynamicTestRegistered(testDescriptor);
                            }
                        """.trimIndent(),
                cc
            )
        )
        val testUniqueId = "testDescriptor.getUniqueId().toString()"
        cc.addMethod(
            CtMethod.make(
                """
                            public void executionSkipped(org.junit.platform.engine.TestDescriptor testDescriptor, String reason) {
                                mainRunner.executionSkipped(testDescriptor, reason);
                                if (!testDescriptor.isContainer()) {
                                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}($testUniqueId);
                                }
                            }
                        """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                            public void executionStarted(org.junit.platform.engine.TestDescriptor testDescriptor) {
                                mainRunner.executionStarted(testDescriptor);
                                if (!testDescriptor.isContainer()) {
                                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}($testUniqueId);
                                }
                            }
                        """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                            public void executionFinished(org.junit.platform.engine.TestDescriptor testDescriptor, org.junit.platform.engine.TestExecutionResult testExecutionResult) {
                                mainRunner.executionFinished(testDescriptor, testExecutionResult);
                                if (!testDescriptor.isContainer()) {
                                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}($testUniqueId, testExecutionResult.getStatus().name());
                                }
                            }
                        """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                            public void reportingEntryPublished(org.junit.platform.engine.TestDescriptor testDescriptor, org.junit.platform.engine.reporting.ReportEntry entry) {
                                mainRunner.reportingEntryPublished(testDescriptor, entry);
                            }
                        """.trimIndent(),
                cc
            )
        )
        cc.toClass(classLoader, protectionDomain)

        ctClass.constructors.first().insertBefore(
            """
                    $1 = new MyList($1);
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}
