package com.epam.drill.test.agent.instrumentation.http.selenium

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.*
import com.epam.drill.test.agent.js.*
import javassist.*
import java.io.*
import java.security.*

const val CHROME_TAB_WITH_PARAMS = "drill.autotesting.params"

class ChromeOptions : Strategy() {
    private val extensionFile: String
    private val dispatcherUrl = AgentConfig.dispatcherUrl().orEmpty()
    private val adminUrl = AgentConfig.adminAddress().orEmpty()
    private val agentId = AgentConfig.agentId().orEmpty()
    private val groupId = AgentConfig.groupId().orEmpty()
    private val agentAddress = AgentConfig.agentUrl().orEmpty()
    private val clientId = ExtensionDispatcher.clientId().orEmpty()

    private val chrome = "org.openqa.selenium.chrome.ChromeOptions"

    init {
        val specifiedPath = AgentConfig.extensionUrl().orEmpty()
        extensionFile = specifiedPath.ifBlank {
            ExtensionLoader().loadExtension().absolutePath
        }
    }

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == chrome
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {

        val agentOrGroup = if (groupId.isBlank()) "agentid=$agentId" else "groupid=$groupId"
        val args = "new-window $CHROME_TAB_WITH_PARAMS?adminurl=$adminUrl&dispatcherurl=$dispatcherUrl" +
                "&$agentOrGroup&clientid=$clientId&agenturl=$agentAddress"
        ctClass.getDeclaredConstructor(emptyArray()).takeIf { extensionFile.isNotBlank() }?.insertAfter(
            """
                    java.io.File extension = new java.io.File("${extensionFile.replace("\\", "\\\\")}");
                    this.addExtensions(com.google.common.collect.ImmutableList.of(extension));
                    this.setExperimentalOption("w3c", new Boolean(false));
                    this.addArguments(com.google.common.collect.ImmutableList.of("$args"));
                """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}
