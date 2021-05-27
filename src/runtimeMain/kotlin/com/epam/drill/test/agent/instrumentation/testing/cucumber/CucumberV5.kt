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
object CucumberV5 : AbstractTestStrategy() {
    const val engineSegment = "[engine:cucumber5]"
    private const val finishedTest = "finishedTest"
    private const val testPackage = "io.cucumber.plugin.event"
    private const val statusPackage = "io.cucumber.plugin.event.Status"

    override val id: String
        get() = "cucumber"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == /*5.x.x*/"io.cucumber.core.runner.TestStep"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {

        val run = ctClass.getDeclaredMethod("run")
        val SpockBus = "SpockBus"
        val cc: CtClass = pool.makeClass(SpockBus)
        cc.interfaces = arrayOf(pool.get("io.cucumber.core.eventbus.EventBus"))
        cc.addField(CtField.make("io.cucumber.core.eventbus.EventBus mainEventBus = null;", cc))
        cc.addField(CtField.make("String featurePath = \"\";", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                    public SpockBus(io.cucumber.core.eventbus.EventBus mainEventBus, String featurePath) {
                        this.mainEventBus = mainEventBus;
                        this.featurePath= featurePath;
                    }
                """.trimMargin(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                    public java.time.Instant getInstant() {
                        return mainEventBus.getInstant();
                    }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                    public java.util.UUID generateId() {
                        return mainEventBus.generateId();
                    }
                """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                    public void send(io.cucumber.plugin.event.Event event) {
                        mainEventBus.send(event);
                        if (event instanceof $testPackage.TestStepStarted) {
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("$engineSegment/[feature:" + featurePath + "]/[scenario:"+(($testPackage.TestStepStarted) event).getTestCase().getName() + "]");    
                        } else {
                            if(event instanceof $testPackage.TestStepFinished) {
                                $testPackage.TestStepFinished $finishedTest = ($testPackage.TestStepFinished) event;
                                $statusPackage status = $getTestStatus
                                ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("$engineSegment/[feature:" + featurePath + "]/[scenario:" + $finishedTest.getTestCase().getName() + "]", status.name());                                    
                            }
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

        cc.addMethod(
            CtMethod.make(
                """
                    public void registerHandlerFor(Class aClass, io.cucumber.plugin.event.EventHandler eventHandler) {
                        mainEventBus.registerHandlerFor(aClass, eventHandler);
                    }
                """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                    public void removeHandlerFor(Class aClass, io.cucumber.plugin.event.EventHandler eventHandler) { 
                        mainEventBus.removeHandlerFor(aClass, eventHandler);
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
        run.insertBefore(
            """
                try {
                    if (stepDefinitionMatch instanceof io.cucumber.core.runner.PickleStepDefinitionMatch) {
                        $getFeaturePath
                        $2 = new $SpockBus($2, featurePath);
                    }
                } catch (Throwable ignored) {}

             """.trimIndent()
        )
        return ctClass.toBytecode()
    }

    private const val getFeaturePath = """
         String[] paths = new java.io.File(".").toURI().relativize($1.getUri()).toString().split(":");
         int index = paths.length - 1;
         String featurePath = paths[index];
         if (featurePath.startsWith("/")) {
            featurePath = featurePath.replaceFirst("/", "");
         }
    """

    private const val getTestStatus = """
        $finishedTest.getResult().getStatus();
        if(status != $statusPackage.PASSED && status != $statusPackage.SKIPPED && status != $statusPackage.FAILED ) {
            status = $statusPackage.FAILED;
        }
    """


}
