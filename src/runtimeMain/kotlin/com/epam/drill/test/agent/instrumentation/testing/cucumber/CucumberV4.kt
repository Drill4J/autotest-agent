package com.epam.drill.test.agent.instrumentation.testing.cucumber

import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import com.epam.drill.test.agent.TestListener
import javassist.*
import java.security.ProtectionDomain

@Suppress("unused")
object CucumberV4 : AbstractTestStrategy() {

    override val id: String
        get() = "cucumber-v4"

    override fun permit(ctClass: CtClass): Boolean {
//        return name == /*5.x.x*/"io.cucumber.core.runner.TestStep" || name == /*4.x.x*/"cucumber.runner.TestStep"
        return false
    }

    override fun instrument(ctClass: CtClass, classLoader: ClassLoader?, protectionDomain: ProtectionDomain?): ByteArray? {
        val pool = ClassPool.getDefault()
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
                                  if (event instanceof cucumber.api.event.TestStepStarted){
                                   ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("[engine:cucumber]/[method:"+((cucumber.api.event.TestStepStarted) event).getTestCase().getName()+"]");
                                  } else if(event instanceof cucumber.api.event.TestStepFinished){
                                   ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}(((cucumber.api.event.TestStepFinished) event).getTestCase().getName(), "xx");
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