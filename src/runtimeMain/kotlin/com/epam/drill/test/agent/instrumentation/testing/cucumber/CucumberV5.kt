package com.epam.drill.test.agent.instrumentation.testing.cucumber

import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import com.epam.drill.test.agent.TestListener
import javassist.*
import java.security.ProtectionDomain
@Suppress("unused")
object CucumberV5 : AbstractTestStrategy() {
    override val id: String
        get() = "cucumber-v5"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == /*5.x.x*/"io.cucumber.core.runner.TestStep"
    }

    override fun instrument(
        ctClass: CtClass,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {

        val run = ctClass.getDeclaredMethod("run")
        val pool = ClassPool.getDefault()
        val SpockBus = "SpockBus"
        val cc: CtClass = pool.makeClass(SpockBus)
        cc.interfaces = arrayOf(pool.get("io.cucumber.core.eventbus.EventBus"))
        cc.addField(CtField.make("io.cucumber.core.eventbus.EventBus mainEventBus = null;", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                                public SpockBus(io.cucumber.core.eventbus.EventBus mainEventBus) {
                                   this.mainEventBus = mainEventBus;
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
                                  if (event instanceof io.cucumber.plugin.event.TestStepStarted){
                                   ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}(((io.cucumber.plugin.event.TestStepStarted) event).getTestCase().getName());
                                  } else if(event instanceof io.cucumber.plugin.event.TestStepFinished){
                                   ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}(((io.cucumber.plugin.event.TestStepFinished) event).getTestCase().getName(), "PASSED");
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
        run.insertBefore(
            """
                        $2 = new $SpockBus($2);
                        
                        """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}