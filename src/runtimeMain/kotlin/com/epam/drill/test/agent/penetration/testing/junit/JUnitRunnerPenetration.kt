package com.epam.drill.test.agent.penetration.testing.junit

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.penetration.AbstractTestStrategy
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
                ${ThreadStorage::class.java.name}.INSTANCE.${ThreadStorage::memorizeTestName.name}(drillTestName);
                
                """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}
