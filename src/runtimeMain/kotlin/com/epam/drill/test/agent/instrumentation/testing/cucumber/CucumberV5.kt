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
    private const val engineSegment = "engine:cucumber5"
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
        protectionDomain: ProtectionDomain?
    ): ByteArray? {

        val run = ctClass.getDeclaredMethod("run")
        val SpockBus = "SpockBus"
        val cc: CtClass = pool.makeClass(SpockBus)
        cc.interfaces = arrayOf(pool.get("io.cucumber.core.eventbus.EventBus"))
        cc.addField(CtField.make("io.cucumber.core.eventbus.EventBus mainEventBus = null;", cc))
        cc.addField(CtField.make("String testPackage = \"\";", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                                public SpockBus(io.cucumber.core.eventbus.EventBus mainEventBus, String testPackage) {
                                   this.mainEventBus = mainEventBus;
                                   this.testPackage = testPackage;
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
                                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("[$engineSegment]/[class:" + testPackage + "]/[method:"+(($testPackage.TestStepStarted) event).getTestCase().getName() + "]");    
                                  } else if(event instanceof $testPackage.TestStepFinished) {
                                    $testPackage.TestStepFinished $finishedTest = ($testPackage.TestStepFinished) event;
                                    $statusPackage status = $getTestStatus
                                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("[$engineSegment]/[class:" + testPackage + "]/[method:" + $finishedTest.getTestCase().getName() + "]", status.name());                                    
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
         *      {@link cucumber.runner.PickleStepDefinitionMatch} is responsible for running tests.
         *      Using this class, we can get meta information about the test, for example, the class in which test located.
         */
        run.insertBefore(
            """
                try {
                    if (stepDefinitionMatch instanceof io.cucumber.core.runner.PickleStepDefinitionMatch) {
                        String testLocation = ((io.cucumber.core.runner.PickleStepDefinitionMatch) stepDefinitionMatch).getStepDefinition().getLocation();
                        $getTestPackages
                        $2 = new $SpockBus($2, testPackage);
                    }
                } catch (Throwable ignored) {}

             """.trimIndent()
        )
        return ctClass.toBytecode()
    }

    private const val getTestPackages = """
         String temp = testLocation.split(" ")[0];
         int bracketIndex = temp.lastIndexOf("(");
         temp = temp.substring(0, bracketIndex);
         int lastIndex = temp.lastIndexOf(".");
         String testPackage = temp.substring(0, lastIndex);
    """

    private const val getTestStatus = """
        $finishedTest.getResult().getStatus();
        if(status != $statusPackage.PASSED && status != $statusPackage.SKIPPED && status != $statusPackage.FAILED ) {
            status = $statusPackage.FAILED;
        }
    """


}
