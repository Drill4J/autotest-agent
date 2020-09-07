package com.epam.drill.test.agent.penetration.testing.junit

import com.epam.drill.test.agent.ThreadStorage
import com.epam.drill.test.agent.penetration.AbstractTestStrategy
import javassist.CtClass

class JUnitPenetration : AbstractTestStrategy() {
    override val id: String = "junit"
    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "org.junit.runners.ParentRunner"
    }

    override fun instrument(ctClass: CtClass): ByteArray? {
        val runChild = ctClass.getDeclaredMethod("runLeaf")
        runChild.insertBefore(
            """
                String drillTestName = $2.getDisplayName();
                ${ThreadStorage::class.java.name}.INSTANCE.${ThreadStorage::memorizeTestName.name}(drillTestName);
                
                """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}