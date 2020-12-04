package com.epam.drill.test.agent.instrumentation.runners

import com.epam.drill.test.agent.instrumentation.*
import javassist.*
import java.security.*
import java.util.*

class JunitRunner : Strategy() {
    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "org.junit.runner.JUnitCore"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        val sessionId = UUID.randomUUID()
        val method = ctClass.getMethod("run", "(Lorg/junit/runner/Runner;)Lorg/junit/runner/Result;")
        method.insertBefore("com.epam.drill.Drill.startSession(\"$sessionId\");")
        method.insertAfter("com.epam.drill.Drill.stopSession(\"$sessionId\");")
        return ctClass.toBytecode()
    }
}
