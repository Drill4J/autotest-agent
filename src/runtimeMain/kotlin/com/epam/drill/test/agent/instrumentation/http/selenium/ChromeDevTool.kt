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

import com.epam.drill.logger.*
import com.epam.drill.test.agent.util.*
import com.github.kklisura.cdt.services.*
import com.github.kklisura.cdt.services.config.*
import com.github.kklisura.cdt.services.impl.*
import com.github.kklisura.cdt.services.invocation.*
import com.github.kklisura.cdt.services.utils.*
import java.lang.reflect.*
import java.net.*
import java.util.concurrent.*

private const val SELENOID_WS_TIMEOUT_SEC: Long = 2
private const val RETRY_ADD_HEADERS_SLEEP_MILLIS: Long = 2000

object DevToolsClientThreadStorage {
    private val logger = Logging.logger(ChromeDevTool::class.java.name)
    private val threadLocalChromeDevTool: InheritableThreadLocal<ChromeDevTool> = InheritableThreadLocal()

    fun addHeaders(headers: Map<*, *>) {
        if (!tryAddHeaders(headers)) {
            //todo remove this workaround
            Thread.sleep(RETRY_ADD_HEADERS_SLEEP_MILLIS)
            if (!tryAddHeaders(headers)) {
                logger.error { "cannot add headers twice. For this test will not collect coverage. Headers: '$headers'" }
            }
        }
    }

    private fun tryAddHeaders(headers: Map<*, *>): Boolean {
        try {
            logger.debug { "try to add headers: $headers" }
            @Suppress("UNCHECKED_CAST")
            getDevTool()?.addHeaders(headers as Map<String, String>)
            logger.debug { "Chrome Tool activated: ${threadLocalChromeDevTool.get() != null}. Headers: $headers" }

        } catch (ex: Exception) {
            logger.warn { "cannot add headers '$headers' because of exception: $ex" }
            return false
        }
        return true
    }

    fun setDevTool(devTool: ChromeDevTool) {
        getDevTool()?.close()
        threadLocalChromeDevTool.set(devTool)
        logger.debug { "DevTool inited for: Thread id=${Thread.currentThread().id}, DevTool instance=$devTool" }
    }

    fun getDevTool(): ChromeDevTool? = threadLocalChromeDevTool.get()

    fun isHeadersAdded() = threadLocalChromeDevTool.get()?.localChromeDevTool?.isHeadersAdded ?: false

    fun resetHeaders() = getDevTool()?.addHeaders(emptyMap())
}

/**
 * Works with local or Selenoid DevTools by websocket
 */
class ChromeDevTool {
    private val logger = Logging.logger(ChromeDevTool::class.java.name)

    //todo it can be create interface ChromeDevTool
    private var selenoidDevTools: SelenoidChromeDevTool = SelenoidChromeDevTool()
    internal var localChromeDevTool: LocalChromeDevTool = LocalChromeDevTool()

    init {
        DevToolsClientThreadStorage.setDevTool(this)
    }

    fun addHeaders(headers: Map<String, String>) {
        trackTime("send headers") {
            localChromeDevTool.addHeaders(headers)
            selenoidDevTools.addHeaders(headers)
        }
    }

    /**
     * connect to remote Selenoid or local webDriver
     */
    fun connect(capabilities: Map<*, *>?, sessionId: String?, remoteHost: String?) = kotlin.runCatching {
        logger.debug { "starting connectToDevTools with cap='$capabilities' sessionId='$sessionId' remote='$remoteHost'..." }
        trackTime("connect to selenoid") {
            remoteHost?.let {
                selenoidDevTools.connect(it, sessionId)
            }
        }
        if (selenoidDevTools.isConnected()) {
            trackTime("connect to local") {
                localChromeDevTool.connect(capabilities)
            }
        }
    }.getOrNull()


    fun close() {
        localChromeDevTool.close()
        selenoidDevTools.close()
    }
}

class SelenoidChromeDevTool {

    private var selenoidDevToolsWs: ChromeDevToolsService? = null

    fun connect(remoteHost: String, sessionId: String?) {
        logger.debug { "connect to selenoid by ws..." }
        val webSocketService = WebSocketServiceImpl.create(URI("ws://$remoteHost/devtools/$sessionId/page"))
        val commandInvocationHandler = CommandInvocationHandler()
        val commandsCache: MutableMap<Method, Any> = ConcurrentHashMap()
        val configuration = ChromeDevToolsServiceConfiguration()
        configuration.readTimeout = SELENOID_WS_TIMEOUT_SEC
        selenoidDevToolsWs = ProxyUtils.createProxyFromAbstract(
            ChromeDevToolsServiceImpl::class.java,
            arrayOf<Class<*>>(
                WebSocketService::class.java,
                ChromeDevToolsServiceConfiguration::class.java
            ),
            arrayOf(webSocketService, configuration)
        ) { _, method: Method, _ ->
            commandsCache.computeIfAbsent(method) {
                ProxyUtils.createProxy(method.returnType, commandInvocationHandler)
            }
        }
        commandInvocationHandler.setChromeDevToolsService(selenoidDevToolsWs)
    }

    fun isConnected() = selenoidDevToolsWs == null || selenoidDevToolsWs?.isClosed == true

    fun addHeaders(headers: Map<String, String>) {
        selenoidDevToolsWs?.network?.let {
            it.setExtraHTTPHeaders(headers)
            it.enable()
        }
    }

    fun close() {
        selenoidDevToolsWs?.let {
            logger.debug { "closing Selenoid ws..." }
            it.close()
        }
    }
}
