package com.epam.drill.test.agent.instrumentation

import com.epam.drill.logger.*
import javassist.ClassPool
import javassist.CtClass
import java.security.ProtectionDomain

abstract class Strategy {
    val logger = Logging.logger { this::class.java.name }

    abstract fun permit(ctClass: CtClass): Boolean

    abstract fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray?
}
