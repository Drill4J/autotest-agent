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

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.*
import javassist.*
import org.objectweb.asm.*
import java.security.*

@Suppress("unused")
object JUnit5Strategy : AbstractTestStrategy() {
    override val id: String
        get() = "junit"

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "org/junit/platform/engine/support/hierarchical/NodeTestTaskContext"
    }

    /*
        Magic constants from junit-platform-engine and junit-jupiter-engine libraries
     */
    // org.junit.platform.engine.UniqueId
    private const val engine = """"engine""""

    // org.junit.jupiter.engine.descriptor.ClassTestDescriptor
    private const val `class` = """"class""""

    // org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor
    private const val method = """"method""""

    // org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor
    private const val testTemplate = """"test-template""""

    // org.junit.jupiter.engine.descriptor.TestTemplateInvocationTestDescriptor
    private const val testTemplateInvocation = """"test-template-invocation""""

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
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
        cc.addMethod(
            CtMethod.make(
                """
                    public void executionSkipped(org.junit.platform.engine.TestDescriptor testDescriptor, String reason) {
                        mainRunner.executionSkipped(testDescriptor, reason);
                        if (!testDescriptor.isContainer()) {
                            ${getSpitedTestName()}
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}(engine, classPath, method, params);
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
                            ${getSpitedTestName()}
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}(engine, classPath, method, params);
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
                            ${getSpitedTestName()}
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}(engine, classPath, method, testExecutionResult.getStatus().name(), params);
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

    private fun getSpitedTestName() = """
        java.util.List segments = testDescriptor.getUniqueId().getSegments();
        if (segments.size() < 3) { return; }
        String engine = null; String classPath = null; String method = null;
        String params = "()"; 
        java.util.Iterator iterator = segments.iterator();
        while (iterator.hasNext()) {
            org.junit.platform.engine.UniqueId.Segment segment = (org.junit.platform.engine.UniqueId.Segment) iterator.next();
            switch (segment.getType()) {
                case $engine:
                    engine = segment.getValue();
                break;
                case $`class`:
                    classPath = segment.getValue();
                break;
                case $method:
                    method = segment.getValue().replace("()", "");
                break;
                case $testTemplate:
                    String fullMethodName = segment.getValue();
                    int bracketIndex = fullMethodName.indexOf("(");
                    params = fullMethodName.substring(bracketIndex);
                    method = fullMethodName.substring(0, bracketIndex);
                break;
                case $testTemplateInvocation:
                    params += segment.getValue();
                break;
                default:
                    /* Unsupported Type */
                break;
            }
        }
    """.trimIndent()
}
