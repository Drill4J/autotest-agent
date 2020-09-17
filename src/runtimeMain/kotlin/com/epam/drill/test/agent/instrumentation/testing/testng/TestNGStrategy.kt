package com.epam.drill.test.agent.instrumentation.testing.testng

import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import com.epam.drill.test.agent.TestListener
import javassist.CtClass
import javassist.CtMethod
import java.security.ProtectionDomain

@Suppress("unused")
object TestNGStrategy : AbstractTestStrategy() {

    override val id: String = "testng"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "org.testng.TestListenerAdapter"
    }

    override fun instrument(
        ctClass: CtClass,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        ctClass.addMethod(CtMethod.make("""
            private static String getParamsString(Object[] parameters, boolean config, int invocationCount) {
    String paramString = "";
    if (parameters.length > 0) {
      if (config) {
        Object parameter = parameters[0];
        if (parameter != null) {
          Class parameterClass = parameter.getClass();
          if (org.testng.ITestResult.class.isAssignableFrom(parameterClass) || org.testng.ITestContext.class.isAssignableFrom(parameterClass) || java.lang.reflect.Method.class.isAssignableFrom(parameterClass)) {
            try {
              paramString = "[" + parameterClass.getMethod("getName",null).invoke(parameter,null) + "]";
            }
            catch (Throwable e) {
              paramString = "";
            }
          }
          else {
            paramString = "[" + parameter.toString() + "]";
          }
        }
      }
      else {
        paramString = java.util.Arrays.deepToString(parameters);
      }
    }
    if (invocationCount > 0) {
      paramString += " (" + invocationCount + ")";
    }
    return paramString.length() > 0 ? paramString : "";
  }
        """.trimIndent(), ctClass))
        sequenceOf(
            ctClass.getDeclaredMethod("onTestSuccess") to "PASSED",
            ctClass.getDeclaredMethod("onTestFailure") to "FAILED",
            ctClass.getDeclaredMethod("onTestSkipped") to "SKIPPED"
        ).forEach { (method, status) ->
            method.insertAfter(
                """
                   ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("[engine:testng]/[class:"+$1.getInstanceName()+"]/[method:"+$1.getName()+getParamsString($1.getParameters(),true,0)+"]", "$status");
            """.trimIndent()
            )
        }
        ctClass.getDeclaredMethod("onTestStart").insertAfter(
            """
            ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("[engine:testng]/[class:"+$1.getInstanceName()+"]/[method:"+$1.getName()+getParamsString($1.getParameters(),true,0)+"]");
        """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}
