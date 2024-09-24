/**
 * Copyright 2020 - 2022 EPAM Systems
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
package com.epam.drill.test.agent.instrument.strategy.testing.testng

import com.epam.drill.test.agent.instrument.strategy.*
import com.epam.drill.test.agent.testinfo.TestController
import javassist.*
import java.security.*

// Only for testng 7.4.0
@Suppress("unused")
object IgnoredTests : AbstractTestStrategy() {
    override val id: String
        get() = "testng"
    private const val IIgnoreAnnotation = "org.testng.annotations.IIgnoreAnnotation"

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "org/testng/internal/annotations/AnnotationHelper"
    }

    /**
     * Support 7.4.0 testng @Ignore annotation
     */
    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        pool.getOrNull(IIgnoreAnnotation)?.also {
            ctClass.getMethod(
                "isAnnotationPresent",
                "(Lorg/testng/internal/annotations/IAnnotationFinder;Ljava/lang/reflect/Method;Ljava/lang/Class;)Z"
            ).insertAfter(
                """ 
            if ($3 == $IIgnoreAnnotation.class && ${'$'}_) {
                ${TestController::class.java.name}.INSTANCE.${TestController::testIgnored.name}("${TestNGStrategy.engineSegment}", $2.getDeclaringClass().getName(), $2.getName());
            }
        """.trimIndent()
            )
        }
        return ctClass.toBytecode()
    }
}
