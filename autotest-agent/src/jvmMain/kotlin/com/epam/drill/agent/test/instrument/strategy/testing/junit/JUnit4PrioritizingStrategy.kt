package com.epam.drill.agent.test.instrument.strategy.testing.junit

import com.epam.drill.agent.test.instrument.strategy.AbstractTestStrategy
import com.epam.drill.agent.test.prioritization.RecommendedTests
import javassist.*
import mu.KotlinLogging
import java.security.ProtectionDomain

private const val Filter = "org.junit.runner.manipulation.Filter"
private const val Description = "org.junit.runner.Description"

@Suppress("unused")
object JUnit4PrioritizingStrategy : AbstractTestStrategy() {
    private val logger = KotlinLogging.logger {}
    private val engineSegment = "junit"
    private val DrillJUnit4Filter = "${this.javaClass.`package`.name}.gen.DrillJUnit4Filter"

    override val id: String
        get() = "junit4Prioritizing"

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "org/junit/runners/JUnit4"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        createRecommendedTestsFilterClass(pool, classLoader, protectionDomain)
        instrumentRunMethod(ctClass)
        return ctClass.toBytecode()
    }

    private fun instrumentRunMethod(ctClass: CtClass) {
        ctClass.constructors.forEach { constructor ->
            constructor.insertAfter(
                """
                $DrillJUnit4Filter drillFilter = new $DrillJUnit4Filter();
                filter(drillFilter);
            """.trimIndent()
            )
        }
    }

    private fun createRecommendedTestsFilterClass(
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): CtClass {
        val cc = pool.makeClass(DrillJUnit4Filter, pool.get(Filter))
        cc.addMethod(
            CtMethod.make(
                """
                   public boolean shouldRun($Description description) {
                        if (!description.isTest()) return true;
                        java.lang.String className = description.getClassName();
                        if (className == null) return true;
                        java.lang.String methodName = description.getMethodName();
                        if (methodName == null) return true;
                        boolean shouldSkip = ${RecommendedTests::class.java.name}.INSTANCE.${RecommendedTests::shouldSkip.name}("$engineSegment", className, methodName, null, null);                            
                        return !shouldSkip;                                                    		                    
                   }
            """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                   public java.lang.String describe() {
                        return "skip tests that are not recommended by Drill4J";                                                    		                    
                   }
            """.trimIndent(),
                cc
            )
        )
        cc.toClass(classLoader, protectionDomain)
        return cc
    }
}