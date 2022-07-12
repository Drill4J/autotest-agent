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
package com.epam.drill.test.agent.instrumentation.http.selenium

import com.epam.drill.agent.instrument.*
import com.epam.drill.test.agent.*
import javassist.*
import org.objectweb.asm.*
import java.io.*
import java.security.*

@Suppress("PrivatePropertyName")
object Selenium : TransformStrategy() {

    private const val Command = "org.openqa.selenium.remote.Command"
    private const val ImmutableMap = "com.google.common.collect.ImmutableMap"
    private const val ImmutableList = "com.google.common.collect.ImmutableList"
    private const val Cookie = "org.openqa.selenium.Cookie"
    private const val DesiredCapabilities = "org.openqa.selenium.remote.DesiredCapabilities"
    private const val Proxy = "org.openqa.selenium.Proxy"
    private const val initPages = "\"about:blank\", \"data:,\""
    private const val EXTENSION_NAME = "header-transmitter.xpi"
    private val FirefoxDriver = "org.openqa.selenium.firefox.FirefoxDriver"

    private val extensionFile: String

    internal const val addDrillCookiesMethod = "addDrillCookies"
    private const val isFirefoxBrowser = "isFirefoxBrowser"



    init {
        val extension = this::class.java.getResource("/$EXTENSION_NAME")
        File(System.getProperty("java.io.tmpdir")).resolve(EXTENSION_NAME).apply {
            extensionFile = absolutePath
            writeBytes(extension.readBytes())
        }
    }

    override fun permit(classReader: ClassReader): Boolean {
        return classReader.className == "org/openqa/selenium/remote/RemoteWebDriver"
    }


    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        logger.debug { "starting instrument ${ctClass.name}..." }

        ctClass.addField(CtField.make("java.lang.String drillRemoteAddress;", ctClass))

        logger.debug { "#####################################" }

        try {
            ctClass
                .getConstructor("(Lorg/openqa/selenium/remote/CommandExecutor;Lorg/openqa/selenium/Capabilities;)V")
                .insertBefore(
                    """
                    java.lang.System.out.println("Constructor called - RemoteWebDriver(CommandExecutor executor, Capabilities desiredCapabilities)");
                """.trimIndent()
                )
        } catch (e: Exception){}

        try {
            ctClass
                .getConstructor("(Lorg/openqa/selenium/Capabilities;)V")
                .insertBefore(
                    """
                    java.lang.System.out.println("Constructor called - RemoteWebDriver(Capabilities desiredCapabilities)");
                """.trimIndent()
                )
        } catch (e: Exception){}

        try {
            ctClass
            .getConstructor("(Ljava/net/URL;Lorg/openqa/selenium/Capabilities;)V")
            .insertBefore(
                """
                drillRemoteAddress = $1.getAuthority();
                java.lang.System.out.println("Constructor called - RemoteWebDriver(URL remoteAddress, Capabilities desiredCapabilities) - drillRemoteAddress: " + $1.getAuthority());
            """.trimIndent()
            )
        } catch (e: Exception){}

        try {
            ctClass
                .getConstructor("(Ljava/net/URL;Lorg/openqa/selenium/Capabilities;Lorg/openqa/selenium/Capabilities;)V")
                .insertBefore(
                    """
                    drillRemoteAddress = $1.getAuthority();
                    java.lang.System.out.println("Constructor called - RemoteWebDriver(URL remoteAddress, Capabilities desiredCapabilities, Capabilities requiredCapabilities) - drillRemoteAddress: " + $1.getAuthority());
                """.trimIndent()
                )
        } catch (e: Exception){}
        logger.debug { "#####################################" }

        ctClass.addMethod(
            CtMethod.make(
                """
                    public boolean $isFirefoxBrowser(org.openqa.selenium.Capabilities capabilities){
                       return org.openqa.selenium.remote.BrowserType.FIREFOX.equals(capabilities.getBrowserName());
                    }
                """.trimIndent(),
                ctClass
            )
        )

        val startSession = ctClass.getDeclaredMethod("startSession")

        /**
         * Browser proxy is needed only for Firefox browser
         */
        startSession.insertBefore(
            """
                if (${AgentConfig::class.java.name}.INSTANCE.${AgentConfig::proxyUrl.name}() != null && $isFirefoxBrowser($1)) {
                    $DesiredCapabilities dCap = new $DesiredCapabilities();
                    $Proxy dProxy = new $Proxy();
                    dProxy.setHttpProxy(${AgentConfig::class.java.name}.INSTANCE.${AgentConfig::proxyUrl.name}());
                    dProxy.setSslProxy(${AgentConfig::class.java.name}.INSTANCE.${AgentConfig::proxyUrl.name}());
                    dCap.setCapability("proxy", dProxy);
                    $1 = $1.merge(dCap);
                }
                ${WebDriverThreadStorage::class.java.name}.INSTANCE.${WebDriverThreadStorage::set.name}(this);
                """
        )
        startSession.insertAfter(
            """
                    if (${AgentConfig::class.java.name}.INSTANCE.${AgentConfig::devToolsProxyAddress.name}() != null){
                        ${ChromeDevTool::class.java.name} drillDevTools = new ${ChromeDevTool::class.java.name}(
                            ((java.util.Map)getCapabilities().getCapability("goog:chromeOptions")),
                            drillRemoteAddress
                        );
                       drillDevTools.${ChromeDevTool::connect.name}(sessionId.toString(), getCurrentUrl());
                    }
                    try {
                        if (this instanceof $FirefoxDriver) {
                            java.util.HashMap hashMapq = new java.util.HashMap();
                            hashMapq.put("path", "${extensionFile.replace("\\", "\\\\")}");
                            hashMapq.put("temporary", Boolean.TRUE);
                            this.execute("installExtension", hashMapq).getValue();
                        }
                    } catch (Exception e){}
            """
        )
        ctClass.addMethod(
            CtMethod.make(
                """
                    public void $addDrillCookiesMethod() {
                        if ($isFirefoxBrowser(getCapabilities()) && $ARE_DRILL_HEADERS_PRESENT) {
                            try {
                                executor.execute(new $Command(sessionId, "addCookie", $ImmutableMap.of("cookie", new $Cookie($SESSION_ID_CALC_LINE))));
                                executor.execute(new $Command(sessionId, "addCookie", $ImmutableMap.of("cookie", new $Cookie($TEST_NAME_CALC_LINE))));
                            } catch(Exception e) { e.printStackTrace();}
                        }
                    }
                """.trimIndent(),
                ctClass
            )
        )
        ctClass.addMethod(
            CtMethod.make(
                """
                    public void addDrillHeaders() {
                        if ($IS_DEV_TOOL_NOT_NULL && $ARE_DRILL_HEADERS_PRESENT && !$IS_HEADER_ADDED) {
                            try {
                                java.util.HashMap hashMap = new java.util.HashMap();
                                hashMap.put($SESSION_ID_CALC_LINE);
                                hashMap.put($TEST_NAME_CALC_LINE);
                                ${getChromeDevTool()}.${ChromeDevTool::addHeaders.name}(hashMap);
                            } catch(Exception e) { e.printStackTrace();}
                        }
                    }
                """.trimIndent(),
                ctClass
            )
        )
        ctClass.getDeclaredMethod("get").insertBefore(
            """
                boolean isInitPage = $ImmutableList.of($initPages).contains(getCurrentUrl());
                if (isInitPage) { execute("get", $ImmutableMap.of("url", $1)); }
                addDrillHeaders();
                $addDrillCookiesMethod();
            """.trimIndent()
        )
        /**
         * todo enable js instrumentation on tab open @Roman_Davliatshin
         */
        ctClass.getMethod(
            "execute",
            "(Ljava/lang/String;Ljava/util/Map;)Lorg/openqa/selenium/remote/Response;"
        ).insertAfter(
            """
                if ($1.equals(org.openqa.selenium.remote.DriverCommand.SWITCH_TO_WINDOW)){
                    java.lang.String currentUrl = getCurrentUrl();
                    if ($IS_DEV_TOOL_NOT_NULL){
                        ${getChromeDevTool()}.${ChromeDevTool::switchSession.name}(currentUrl);
                    } else {
                        execute("get", $ImmutableMap.of("url",currentUrl));
                        $addDrillCookiesMethod();
                    }
                }
            """.trimIndent()
        )
        ctClass.getDeclaredMethod("quit").insertBefore(
            """
                if ($IS_DEV_TOOL_NOT_NULL){
                    ${getChromeDevTool()}.${ChromeDevTool::close.name}();
                    ${DevToolStorage::class.java.name}.INSTANCE.${DevToolStorage::clear.name}();
                }
                ${WebDriverThreadStorage::class.java.name}.INSTANCE.${WebDriverThreadStorage::clear.name}();
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }

    private fun getChromeDevTool() = "${DevToolStorage::class.java.name}.INSTANCE.${DevToolStorage::get.name}()"
}
