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

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.*
import javassist.*
import java.security.*

private enum class MethodSignature(val signature: String) {
    Cucumber5("(Lio/cucumber/plugin/event/TestCase;Lio/cucumber/core/eventbus/EventBus;Lio/cucumber/core/runner/TestCaseState;ZLjava/util/UUID;)Z"),
    Cucumber6("(Lio/cucumber/plugin/event/TestCase;Lio/cucumber/core/eventbus/EventBus;Lio/cucumber/core/runner/TestCaseState;Z)Z"),
    Cucumber6_7("(Lio/cucumber/plugin/event/TestCase;Lio/cucumber/core/eventbus/EventBus;Lio/cucumber/core/runner/TestCaseState;Lio/cucumber/core/runner/ExecutionMode;)Lio/cucumber/core/runner/ExecutionMode;")
}


@Suppress("unused")
object Cucumber : AbstractTestStrategy() {
    const val engineSegmentCucumber5 = "[engine:cucumber5]"
    const val engineSegmentCucumber6 = "[engine:cucumber6]"
    private const val finishedTest = "finishedTest"
    private const val testPackage = "io.cucumber.plugin.event"
    private const val statusPackage = "io.cucumber.plugin.event.Status"
    private const val eventBusProxy = "EventBusProxy"

    override val id: String
        get() = "cucumber"

    /**
     * From cucumber 5 TestStep class location doesn't change
     */
    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "io.cucumber.core.runner.TestStep"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {

        val (methodToReTransform, sendMethod) = determinateMethodToRetransform(ctClass) ?: return ctClass.toBytecode()
        val eventBusProxyClass: CtClass = createEventBusProxyClass(pool, sendMethod)
        eventBusProxyClass.toClass(classLoader, protectionDomain)

        /**
         *      {@link cucumber.runner.PickleStepDefinitionMatch} is represent a step of scenario.
         *      Check for PickleStepDefinitionMatch is needed to determine what we are currently performing,
         *      a step from a scenario or before or after action.
         *      Instead of the class name, we use the path to the feature file.
         *      If the file is in the same repository as the tests, then we take the relative path,
         *      otherwise we take the absolute path without specifying the disk name
         */
        methodToReTransform.insertBefore(
            """
                try {
                    if (stepDefinitionMatch instanceof io.cucumber.core.runner.PickleStepDefinitionMatch) {
                        $getFeaturePath
                        $2 = new $eventBusProxy($2, featurePath);
                    }
                } catch (Throwable ignored) {}

             """.trimIndent()
        )
        return ctClass.toBytecode()
    }

    private fun createEventBusProxyClass(
        pool: ClassPool,
        sendMethod: String,
    ): CtClass = pool.makeClass(eventBusProxy).apply {
        interfaces = arrayOf(pool.get("io.cucumber.core.eventbus.EventBus"))
        addField(CtField.make("io.cucumber.core.eventbus.EventBus mainEventBus = null;", this))
        addField(CtField.make("String featurePath = \"\";", this))
        addConstructor(
            CtNewConstructor.make(
                """
                        public SpockBus(io.cucumber.core.eventbus.EventBus mainEventBus, String featurePath) {
                            this.mainEventBus = mainEventBus;
                            this.featurePath= featurePath;
                        }
                    """.trimMargin(),
                this
            )
        )

        addMethod(
            CtMethod.make(
                """
                        public java.time.Instant getInstant() {
                            return mainEventBus.getInstant();
                        }
                    """.trimIndent(),
                this
            )
        )
        addMethod(
            CtMethod.make(
                """
                        public java.util.UUID generateId() {
                            return mainEventBus.generateId();
                        }
                    """.trimIndent(),
                this
            )
        )

        addMethod(CtMethod.make(sendMethod, this))

        addMethod(
            CtMethod.make(
                """
                        public void sendAll(Iterable queue) {
                            mainEventBus.sendAll(queue);
                        }
                    """.trimIndent(),
                this
            )
        )

        addMethod(
            CtMethod.make(
                """
                        public void registerHandlerFor(Class aClass, io.cucumber.plugin.event.EventHandler eventHandler) {
                            mainEventBus.registerHandlerFor(aClass, eventHandler);
                        }
                    """.trimIndent(),
                this
            )
        )

        addMethod(
            CtMethod.make(
                """
                        public void removeHandlerFor(Class aClass, io.cucumber.plugin.event.EventHandler eventHandler) { 
                            mainEventBus.removeHandlerFor(aClass, eventHandler);
                        }
                    """.trimIndent(),
                this
            )
        )
    }

    private fun determinateMethodToRetransform(
        ctClass: CtClass,
    ) = runCatching {
        ctClass.getMethod("run", MethodSignature.Cucumber5.signature) to sendMethod("io.cucumber.plugin.event.Event",
            engineSegmentCucumber5)
    }.getOrNull() ?: runCatching {
        ctClass.getMethod("run", MethodSignature.Cucumber6.signature) to sendMethod(java.lang.Object::class.java.name,
            engineSegmentCucumber6)
    }.getOrNull() ?: runCatching {
        ctClass.getMethod("run", MethodSignature.Cucumber6_7.signature) to sendMethod(java.lang.Object::class.java.name,
            engineSegmentCucumber6)
    }.getOrNull()


    private fun sendMethod(paramType: String, engineSegment: String) = ("""
                public void send($paramType event) {
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
                """.trimIndent()
            )


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
        if (status != $statusPackage.PASSED) {
            status = $statusPackage.FAILED;
        }
    """


}
