package com.epam.drill.test.agent.instrumentation

import javassist.CtClass
import java.security.ProtectionDomain

abstract class Strategy {

    abstract fun permit(ctClass: CtClass): Boolean

    abstract fun instrument(
        ctClass: CtClass,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray?
}
