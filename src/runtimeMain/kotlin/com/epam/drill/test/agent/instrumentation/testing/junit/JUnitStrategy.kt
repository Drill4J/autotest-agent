package com.epam.drill.test.agent.instrumentation.testing.junit

import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import com.epam.drill.test.agent.instrumentation.testing.TestListener
import javassist.*
import java.security.ProtectionDomain

@Suppress("unused")
object JUnitStrategy : AbstractTestStrategy() {
    override val id: String = "junit"
    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "org.junit.runner.notification.RunNotifier"
    }

    override fun instrument(
        ctClass: CtClass,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {

        val pool = ClassPool.getDefault()
        val cc: CtClass = pool.makeClass("MyList")
        cc.superclass = pool.get("org.junit.runner.notification.RunListener")
        cc.addField(CtField.make("org.junit.runner.notification.RunListener mainRunner = null;", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                            public MyList(org.junit.runner.notification.RunListener mainRunner) {
                               this.mainRunner = mainRunner;
                            }
                        """.trimMargin()
                , cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                              public void testRunStarted(org.junit.runner.Description description) throws Exception {
                                  this.mainRunner.testRunStarted(description);
                              }
                        """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                              public void testStarted(org.junit.runner.Description description) throws Exception {
                                this.mainRunner.testStarted(description);
                                
                                 ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("[engine:junit]/[class:"+description.getClassName()+"]/[method:"+description.getMethodName()+"]");
                              }
                        """.trimIndent(),
                cc
            )
        )


        cc.addMethod(
            CtMethod.make(
                """
                            public void testFinished(org.junit.runner.Description description) throws Exception {
                                this.mainRunner.testFinished(description);
                                ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("[engine:junit]/[class:"+description.getClassName()+"]/[method:"+description.getMethodName()+"]", "SUCCESSFULLY");
                              }
                        """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                              public void testRunFinished(org.junit.runner.Result result) throws Exception {
                                this.mainRunner.testRunFinished(result);
                              }
                        """.trimIndent(),
                cc
            )
        )


        cc.addMethod(
            CtMethod.make(
                """
                               public void testFailure(org.junit.runner.notification.Failure failure) throws Exception {
                                  this.mainRunner.testFailure(failure);
                                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("[engine:junit]/[class:"+failure.getDescription().getClassName()+"]/[method:"+failure.getDescription().getMethodName()+"]", "FAILURE");
                              }
                        """.trimIndent(),
                cc
            )
        )


        cc.addMethod(
            CtMethod.make(
                """
                               public void testAssumptionFailure(org.junit.runner.notification.Failure failure) {
                                  this.mainRunner.testAssumptionFailure(failure);
                              }
                        """.trimIndent(),
                cc
            )
        )



        cc.addMethod(
            CtMethod.make(
                """
                              public void testIgnored(org.junit.runner.Description description) throws Exception {
                                    this.mainRunner.testIgnored(description);
                              }
                        """.trimIndent(),
                cc
            )
        )



        cc.toClass(classLoader, protectionDomain)
        ctClass.getDeclaredMethod("addListener").insertBefore(
            """
                $1= new MyList($1);
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}