package com.epam.drill.test.agent.penetration.testing.spock

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.penetration.*
import javassist.*

class Spock : AbstractTestStrategy() {
    override val id: String
        get() = "spock"

    private val baseRunner = "org.spockframework.runtime.BaseSpecRunner"
    private val platformRunner = "org.spockframework.runtime.PlatformSpecRunner"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == baseRunner || ctClass.name == platformRunner
    }

    override fun instrument(ctClass: CtClass): ByteArray? {
        val createMethodInfo = ctClass.getDeclaredMethod("createMethodInfoForDoRunFeature")
        when (ctClass.name) {
            baseRunner -> createMethodInfo.insertBefore(
                """
                    String testName = currentFeature.getDescription().toString();
                 ${ThreadStorage::class.java.name}.INSTANCE.${ThreadStorage::memorizeTestName.name}(testName);
            """.trimIndent()
            )
            platformRunner -> createMethodInfo.insertBefore(
                """
                    String testName = $1.getCurrentFeature().getName() + "("+ $1.getCurrentFeature().getParent().getName()+")";
                 ${ThreadStorage::class.java.name}.INSTANCE.${ThreadStorage::memorizeTestName.name}(testName);
            """.trimIndent()
            )
        }
        return ctClass.toBytecode()
    }

}

