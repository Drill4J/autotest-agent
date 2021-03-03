/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.test.agent.instrumentation.testing.cucumber

import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import javassist.*
import java.security.ProtectionDomain

@Suppress("unused")
object RunnersInstrumentation : AbstractTestStrategy() {

    override val id: String
        get() = "cucumber-runners"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "io.cucumber.testng.AbstractTestNGCucumberTests" || ctClass.name == "io.cucumber.junit.Cucumber"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? = when (ctClass.name) {
        "io.cucumber.testng.AbstractTestNGCucumberTests" -> abstractTestNGCucumberTestsInstrumentation(ctClass)
        "io.cucumber.junit.Cucumber" -> cucumberInstrumentation(ctClass)
        else -> ctClass.toBytecode()
    }

    private fun abstractTestNGCucumberTestsInstrumentation(
        ctClass: CtClass,
    ): ByteArray {
        ctClass.getDeclaredMethod("setUpClass")
            .insertBefore("${CucumberUtil::class.java.name}.INSTANCE.${CucumberUtil::setValue.name}(false);")
        return ctClass.toBytecode()
    }

    private fun cucumberInstrumentation(
        ctClass: CtClass,
    ): ByteArray {
        ctClass.getConstructor("(Ljava/lang/Class;)V")
            .insertBefore("${CucumberUtil::class.java.name}.INSTANCE.${CucumberUtil::setValue.name}(false);")
        return ctClass.toBytecode()
    }


}
