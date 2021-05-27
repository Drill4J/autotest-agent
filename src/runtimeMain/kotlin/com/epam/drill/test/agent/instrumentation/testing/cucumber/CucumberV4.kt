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
package com.epam.drill.test.agent.instrumentation.testing.cucumber

import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import com.epam.drill.test.agent.TestListener
import javassist.*
import java.security.ProtectionDomain

@Suppress("unused")
object CucumberV4 : AbstractTestStrategy() {
    const val engineSegment = "[engine:cucumber4]"
    private const val finishedTest = "finishedTest"
    private const val statusPackage = "cucumber.api.Result.Type"
    private const val testPackage = "cucumber.api.event"

    override val id: String
        get() = "cucumber"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == /*4.x.x*/"cucumber.runner.TestStep"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        val SpockBus = "SpockBus"
        val cc: CtClass = pool.makeClass(SpockBus)
        cc.interfaces = arrayOf(pool.get("cucumber.runner.EventBus"))
        cc.addField(CtField.make("cucumber.runner.EventBus mainEventBus = null;", cc))
        cc.addField(CtField.make("String featurePath = \"\";", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                    public $SpockBus(cucumber.runner.EventBus mainEventBus, String featurePath) { 
                        this.mainEventBus = mainEventBus;
                        this.featurePath = featurePath;
                    }
                """.trimMargin(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                    public Long getTime() {
                        return mainEventBus.getTime();
                    }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                    public Long getTimeMillis() {
                        return mainEventBus.getTimeMillis();
                    }
                """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                    public void send(cucumber.api.event.Event event) {
                        mainEventBus.send(event);   
                        if (event instanceof $testPackage.TestStepStarted) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("${engineSegment}/[feature:" + featurePath + "]/[scenario:"+(($testPackage.TestStepStarted) event).getTestCase().getName() + "]");    
                        } else if (event instanceof $testPackage.TestStepFinished) {
                            $testPackage.TestStepFinished $finishedTest = ($testPackage.TestStepFinished) event;
                            $statusPackage status = $getTestStatus
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("${engineSegment}/[feature:" + featurePath + "]/[scenario:" + $finishedTest.getTestCase().getName() + "]", status.name());                                    
                        }
                    }
                """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                    public void sendAll(Iterable queue) {
                        mainEventBus.sendAll(queue);
                                  
                    }
                """.trimIndent(),
                cc
            )
        )
        cc.toClass(classLoader, protectionDomain)

        /**
         *      {@link cucumber.runner.PickleStepDefinitionMatch} is represent a step of scenario.
         *      Check for PickleStepDefinitionMatch is needed to determine what we are currently performing,
         *      a step from a scenario or before or after action.
         *      Instead of the class name, we use the path to the feature file.
         *      If the file is in the same repository as the tests, then we take the relative path,
         *      otherwise we take the absolute path without specifying the disk name
         */
        ctClass.getDeclaredMethod("run").insertBefore(
            """
                try {
                    if (stepDefinitionMatch instanceof cucumber.runner.PickleStepDefinitionMatch) {
                        $getFeaturePath
                        $2 = new SpockBus($2, featurePath);
                    }
                } catch (Throwable ignored) {}
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }


    private const val getFeaturePath = """
         String[] paths = new java.io.File(".").toURI().resolve($1.getUri()).toString().split(":");
         int index = paths.length - 1;
         String featurePath = paths[index];
         if (featurePath.startsWith("/")) {
            featurePath = featurePath.replaceFirst("/", "");
         }
    """
    private const val getTestStatus = """
        $finishedTest.result.getStatus();
        if(status != $statusPackage.PASSED && status != $statusPackage.SKIPPED && status != $statusPackage.FAILED ) {
            status = $statusPackage.FAILED;
        }
    """

}
