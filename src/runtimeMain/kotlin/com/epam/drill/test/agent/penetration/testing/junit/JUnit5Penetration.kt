package com.epam.drill.test.agent.penetration.testing.junit

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.penetration.*
import javassist.*

class JUnit5Penetration : AbstractTestStrategy() {
    override val id: String = "junit5"
    val invoker = "org.junit.jupiter.engine.execution.ExecutableInvoker"
    val interceptor = "org.junit.jupiter.api.extension.InvocationInterceptor"
    override fun permit(ctClass: CtClass): Boolean {


        return ctClass.name == invoker ||
                ctClass.name == interceptor
    }

    override fun instrument(ctClass: CtClass): ByteArray? {
        return when (ctClass.name) {
            invoker -> {
                ctClass.getDeclaredMethods("invoke").forEach {
                    it.insertBefore(
                        method("$3")
                    )
                }
                ctClass.toBytecode()
            }
            interceptor -> {
                ctClass.getDeclaredMethod("interceptDynamicTest").insertBefore(
                    method("$2")
                )
                ctClass.toBytecode()
            }
            else -> null
        }

    }

    private fun method(vari:String): String {
        return """
                     try {
                        if (Class.forName("org.junit.jupiter.engine.descriptor.DynamicExtensionContext").isInstance($vari)||Class.forName("org.junit.jupiter.engine.descriptor.MethodExtensionContext").isInstance($vari)){
                            org.junit.jupiter.api.extension.ExtensionContext parentExtension = $vari.getParent().get();
                            String drillTestName;
                            if (Class.forName("org.junit.jupiter.engine.descriptor.ClassExtensionContext").isInstance(parentExtension)){
                                drillTestName = parentExtension.getDisplayName()+":"+$vari.getDisplayName().replace("()", "");
                            } else {
                                org.junit.jupiter.api.extension.ExtensionContext rootExtension = parentExtension.getParent().get();
                                drillTestName = rootExtension.getDisplayName()+":"+parentExtension.getDisplayName().replace("()", "")+":"+$vari.getDisplayName();
                            }
                            ${ThreadStorage::class.java.name}.INSTANCE.${ThreadStorage::memorizeTestName.name}(drillTestName);
                        }
                     } catch (Exception ex) {}
                    """.trimIndent()
    }
}
