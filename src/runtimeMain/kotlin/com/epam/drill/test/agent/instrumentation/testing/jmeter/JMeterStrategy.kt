package com.epam.drill.test.agent.instrumentation.testing.jmeter

import com.epam.drill.test.agent.AgentClassTransformer
import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import javassist.CtClass
import java.security.ProtectionDomain

@Suppress("unused")
object JMeterStrategy : AbstractTestStrategy() {
    private val testNameSourceClass = "org.apache.jmeter.protocol.http.sampler.HTTPHC4Impl"
    override val id: String = "jmeter"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == testNameSourceClass
    }

    override fun instrument(
        ctClass: CtClass,
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
