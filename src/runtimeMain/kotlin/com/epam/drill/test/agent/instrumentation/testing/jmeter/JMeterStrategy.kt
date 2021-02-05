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
package com.epam.drill.test.agent.instrumentation.testing.jmeter

import com.epam.drill.test.agent.AgentClassTransformer
import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import javassist.ClassPool
import javassist.CtClass
import java.security.ProtectionDomain

@Suppress("unused")
object JMeterStrategy : AbstractTestStrategy() {
    private val testNameSourceClass = "org.apache.jmeter.protocol.http.sampler.HTTPHC4Impl"
    override val id: String
        get() = "jmeter"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == testNameSourceClass
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        val setupRequestMethod = ctClass.getMethod(
            "setupRequest",
            "(Ljava/net/URL;Lorg/apache/http/client/methods/HttpRequestBase;" +
                    "Lorg/apache/jmeter/protocol/http/sampler/HTTPSampleResult;)V"
        )
        setupRequestMethod.insertBefore(
            """
                String drillTestName = $3.getSampleLabel();
                ${AgentClassTransformer.CLASS_NAME}.memorizeTestName(drillTestName);
                
                """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}
