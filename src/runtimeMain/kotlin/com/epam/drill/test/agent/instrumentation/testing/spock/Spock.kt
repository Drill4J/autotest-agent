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
package com.epam.drill.test.agent.instrumentation.testing.spock

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.*
import javassist.*
import java.security.ProtectionDomain

@Suppress("unused")
object Spock : AbstractTestStrategy() {
    override val id: String
        get() = "spock"

    private val baseRunner = "org.spockframework.runtime.BaseSpecRunner"
    private val platformRunner = "org.spockframework.runtime.PlatformSpecRunner"

    override fun permit(ctClass: CtClass): Boolean {
//        return ctClass.name == baseRunner || ctClass.name == platformRunner
        return false
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        val createMethodInfo = ctClass.getDeclaredMethod("createMethodInfoForDoRunFeature")
        when (ctClass.name) {
            //TODO need implementation for com.epam.drill.test.agent.TestListener
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

