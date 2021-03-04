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
    private const val engineSegment = "engine:cucumber4"
    private const val finishedTest = "finishedTest"
    private const val statusPackage = "cucumber.api.Result.Type"
    private const val testPackage = "cucumber.api.event"

    override val id: String
        get() = "cucumber-v4"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == /*4.x.x*/"cucumber.runner.TestStep"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        val SpockBus = "SpockBus"
        val cc: CtClass = pool.makeClass(SpockBus)
        cc.interfaces = arrayOf(pool.get("cucumber.runner.EventBus"))
        cc.addField(CtField.make("cucumber.runner.EventBus mainEventBus = null;", cc))
        cc.addField(CtField.make("String testPackage = \"\";", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                                public $SpockBus(cucumber.runner.EventBus mainEventBus, String testPackage) { 
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
                                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("[${engineSegment}]/[class:" + testPackage + "]/[method:"+(($testPackage.TestStepStarted) event).getTestCase().getName() + "]");    
                                  } else if (event instanceof $testPackage.TestStepFinished) {
                                    $testPackage.TestStepFinished $finishedTest = ($testPackage.TestStepFinished) event;
                                    $statusPackage status = $getTestStatus
                                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("[${engineSegment}]/[class:" + testPackage + "]/[method:" + $finishedTest.getTestCase().getName() + "]", status.name());                                    
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
         *      {@link cucumber.runner.PickleStepDefinitionMatch} is responsible for running tests.
         *      Using this class, we can get meta information about the test, for example, the class in which test located.
         *      Since in the implementation of cucumber 4 we can't get the full path to the class ,
         *      we can't support the use of Before and After annotations
         *      See {@link cucumber.runner.HookDefinitionMatch} specifically the implementation of the method {@link cucumber.runner.HookDefinitionMatch#getCodeLocation()}
         */
        ctClass.getDeclaredMethod("run").insertBefore(
            """
                try {
                    if (stepDefinitionMatch instanceof cucumber.runner.PickleStepDefinitionMatch) {
                        String testLocation = ((cucumber.runner.PickleStepDefinitionMatch) stepDefinitionMatch).getStepDefinition().getLocation(true);
                        $getTestPackages
                        $2 = new SpockBus($2, testPackage);
                    }
                } catch (Throwable ignored) {}
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }

    private const val getTestPackages = """
         String temp = testLocation.split(" ")[0];
         int lastIndex = temp.lastIndexOf(".");
         String testPackage = temp.substring(0, lastIndex);
    """

    private const val getTestStatus = """
        $finishedTest.result.getStatus();
        if(status != $statusPackage.PASSED && status != $statusPackage.SKIPPED && status != $statusPackage.FAILED ) {
            status = $statusPackage.FAILED;
        }
    """

}
