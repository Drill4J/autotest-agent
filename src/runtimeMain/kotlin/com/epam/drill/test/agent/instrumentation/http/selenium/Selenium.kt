package com.epam.drill.test.agent.instrumentation.http.selenium

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.*
import javassist.*
import java.io.*
import java.security.*

const val EXTENSION_NAME = "header-transmitter.xpi"

@Suppress("PrivatePropertyName")
class Selenium : Strategy() {
    private val extensionFile: String
    private val Command = "org.openqa.selenium.remote.Command"
    private val ImmutableMap = "com.google.common.collect.ImmutableMap"
    private val Cookie = "org.openqa.selenium.Cookie"
    private val DesiredCapabilities = "org.openqa.selenium.remote.DesiredCapabilities"
    private val Proxy = "org.openqa.selenium.Proxy"

    init {
        val extension = this::class.java.getResource("/$EXTENSION_NAME")
//        val extensionInstallationDir = defineInstallationDir(extension.file)
        File(System.getProperty("java.io.tmpdir")).resolve(EXTENSION_NAME).apply {
            extensionFile = absolutePath
            writeBytes(extension.readBytes())
        }

    }

    private fun defineInstallationDir(rawPath: String): String {
        return rawPath.removePrefix("file:")
            .removeSuffix("drillRuntime.jar!/$EXTENSION_NAME")
    }

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "org.openqa.selenium.remote.RemoteWebDriver"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        ctClass.addField(
            CtField.make(
                "${ChromeDevTool::class.java.name} drillDevTools=(${ChromeDevTool::class.java.name}) new ${ChromeDevTool::class.java.name}();",
                ctClass
            ).apply { modifiers = 9 })
        ctClass.addField(
            CtField.make("boolean drillIsGet = false;", ctClass).apply { modifiers = 9 })
        val startSession = ctClass.getDeclaredMethod("startSession")

        startSession.insertBefore(
            """
                if (${ThreadStorage::class.java.name}.INSTANCE.${ThreadStorage::proxyUrl.name}() != null) {
                    $DesiredCapabilities dCap = new $DesiredCapabilities();
                    $Proxy dProxy = new $Proxy();
                    dProxy.setHttpProxy(${ThreadStorage::class.java.name}.INSTANCE.${ThreadStorage::proxyUrl.name}());
                    dCap.setCapability("proxy", dProxy);
                    $1 = $1.merge(dCap);
                }
                if ($IF_CONDITION) {
                    try {
                        org.openqa.selenium.remote.Response a = executor.execute(new $Command(sessionId, "addCookie", $ImmutableMap.of("cookie", new $Cookie($SESSION_ID_CALC_LINE))));
                        org.openqa.selenium.remote.Response b = executor.execute(new $Command(sessionId, "addCookie", $ImmutableMap.of("cookie", new $Cookie($TEST_NAME_CALC_LINE))));
                    } catch(Exception e) {}
                } 
                """
        )
        startSession.insertAfter(
            """
                    drillDevTools.${ChromeDevTool::connectToDevTools.name}(((java.util.Map)getCapabilities().getCapability("goog:chromeOptions")));
                    try {
                        if (this instanceof org.openqa.selenium.firefox.FirefoxDriver) {
                            java.util.HashMap hashMapq = new java.util.HashMap();
                            hashMapq.put("path", "${extensionFile.replace("\\", "\\\\")}");
                            hashMapq.put("temporary", Boolean.TRUE);
                            this.execute("installExtension", hashMapq).getValue();
                        }
                    } catch (Exception e){}
                """
        )
        ctClass.getDeclaredMethod("get").insertBefore(
            """
                if ($IF_CONDITION) {
                    try {
                        java.util.HashMap hashMap = new java.util.HashMap();
                        hashMap.put($SESSION_ID_CALC_LINE);
                        hashMap.put($TEST_NAME_CALC_LINE);
                        ${DevToolsClientThreadStorage::class.java.name}.INSTANCE.${DevToolsClientThreadStorage::addHeaders.name}(hashMap);
                    } catch(Exception e) { e.printStackTrace();}
                }
            """.trimIndent()
        )
        ctClass.getDeclaredMethod("quit").insertBefore(
                """
                    drillDevTools.${ChromeDevTool::close.name}();
                """.trimIndent()
            )
        return ctClass.toBytecode()
    }

}
