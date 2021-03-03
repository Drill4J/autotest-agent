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
import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import javassist.*
import java.security.ProtectionDomain

@Suppress("unused")
object CucumberV4 : AbstractTestStrategy() {

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
        cc.addConstructor(
            CtNewConstructor.make(
                """
                                public $SpockBus(cucumber.runner.EventBus mainEventBus) { 
                                   this.mainEventBus = mainEventBus;
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
                                  if (${CucumberUtil::class.java.name}.INSTANCE.${CucumberUtil::isNotStartedByRunner.name}()) {
                                    if (event instanceof cucumber.api.event.TestStepStarted){
                                        ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("[engine:cucumber]/[method:"+((cucumber.api.event.TestStepStarted) event).getTestCase().getName()+"]");
                                    } else if(event instanceof cucumber.api.event.TestStepFinished){
                                        ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}(((cucumber.api.event.TestStepFinished) event).getTestCase().getName(), "PASSED");
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
        cc.toClass(classLoader, protectionDomain)
        ctClass.getDeclaredMethod("run").insertBefore(
            """
                $2 = new SpockBus($2);
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}
