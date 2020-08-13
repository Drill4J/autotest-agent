package com.epam.drill.test.agent.penetration.testing.cucumber

import com.epam.drill.test.agent.ThreadStorage
import com.epam.drill.test.agent.penetration.AbstractTestStrategy
import javassist.CtClass

class Cucumber : AbstractTestStrategy() {
    override val id: String
        get() = "cucumber"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "io.cucumber.core.runner.TestStep"
    }

    override fun instrument(ctClass: CtClass): ByteArray? {
        ctClass.getDeclaredMethod("run").insertBefore("""
                ${ThreadStorage::class.java.name}.INSTANCE.${ThreadStorage::memorizeTestName.name}($1.getName());
        """.trimIndent())
        return ctClass.toBytecode()
    }
}