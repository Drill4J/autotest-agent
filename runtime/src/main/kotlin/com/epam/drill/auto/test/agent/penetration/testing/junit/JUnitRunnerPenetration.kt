package com.epam.drill.auto.test.agent.penetration.testing.junit

import com.epam.drill.auto.test.agent.AgentClassTransformer
import com.epam.drill.auto.test.agent.penetration.AbstractTestStrategy
import javassist.CtClass

class JUnitRunnerPenetration : AbstractTestStrategy() {
    override val id: String = "junit-runner"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.interfaces.any { it.name == "org.junit.platform.engine.support.hierarchical.Node\$DynamicTestExecutor" }
    }

    override fun instrument(ctClass: CtClass): ByteArray? {
        val runChild = ctClass.getDeclaredMethod("execute")
        runChild.insertBefore(
            """
                String drillTestName = $1.getDisplayName();
                ${AgentClassTransformer.CLASS_NAME}.INSTANCE.memorizeTestName(drillTestName);
                
                """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}