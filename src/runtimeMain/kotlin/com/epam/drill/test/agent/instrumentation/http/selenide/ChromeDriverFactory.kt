package com.epam.drill.test.agent.instrumentation.http.selenide

import com.epam.drill.test.agent.instrumentation.*
import javassist.*
import java.security.*

class ChromeDriverFactory : Strategy() {
    private val factory = "com.codeborne.selenide.webdriver.ChromeDriverFactory"

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == factory
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        ctClass.getDeclaredMethod("excludeSwitches").setBody(
            """
                return new String[]{"enable-automation"};
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}
