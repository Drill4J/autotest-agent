package com.epam.drill.agent.test.instrument.strategy.testing.testng

import com.epam.drill.agent.test.instrument.strategy.AbstractTestStrategy
import com.epam.drill.agent.test.prioritization.RecommendedTests
import javassist.ClassPool
import javassist.CtClass
import mu.KLogger
import java.security.ProtectionDomain

abstract class TestNGPrioritizingStrategy : AbstractTestStrategy() {
    private val engineSegment = "testng"
    abstract val logger: KLogger
    abstract val versionRegex: Regex
    abstract fun getMethodParametersExpression(): String

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return interfaces.any { it == "org/testng/IMethodSelector" }
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        return if ("${ctClass.url}".contains(versionRegex)) {
            instrumentIfSupport(ctClass, pool, classLoader, protectionDomain)
        } else {
            null
        }
    }

    private fun instrumentIfSupport(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        instrumentIncludeMethod(ctClass)
        return ctClass.toBytecode()
    }

    private fun instrumentIncludeMethod(ctClass: CtClass) {
        ctClass.getMethod(
            "includeMethod",
            "(Lorg/testng/IMethodSelectorContext;Lorg/testng/ITestNGMethod;Z)Z"
        ).insertAfter(
            """            
            if (${'$'}_ == true && $3 == true) {                
                java.lang.String className = $2.getTestClass().getName();
                java.lang.String methodName = $2.getMethodName();
                java.lang.String methodParameters = ${this::class.java.name}.INSTANCE.${this::paramTypes.name}($2.${getMethodParametersExpression()});                
                boolean shouldSkip = ${RecommendedTests::class.java.name}.INSTANCE.${RecommendedTests::shouldSkip.name}("$engineSegment", className, methodName, methodParameters, null);
                if (shouldSkip) {
                    return false;
                }
            }                        
        """.trimIndent()
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun paramTypes(objects: Array<Class<*>?>?): String = objects?.joinToString(",", "(", ")") { obj ->
        obj?.name ?: ""
    } ?: ""

}