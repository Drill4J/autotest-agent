package com.epam.drill.test.agent.penetration.testing.spock

import com.epam.drill.test.agent.ThreadStorage
import com.epam.drill.test.agent.penetration.AbstractTestStrategy
import javassist.CtClass

class Spock : AbstractTestStrategy() {
    override val id: String
        get() = "spock"

    override fun permit(ctClass: CtClass): Boolean {
       return ctClass.name == "org.spockframework.runtime.BaseSpecRunner"
    }

    override fun instrument(ctClass: CtClass): ByteArray? {
        ctClass.getDeclaredMethod("createMethodInfoForDoRunFeature")
            .insertBefore("""
                 ${ThreadStorage::class.java.name}.INSTANCE.${ThreadStorage::memorizeTestName.name}(currentFeature.getDescription().toString());
            """.trimIndent())
        return ctClass.toBytecode()
    }

}

