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
import com.epam.drill.test.agent.config.*
import com.epam.drill.test.agent.util.*
import com.github.kklisura.cdt.services.*
import com.github.kklisura.cdt.services.config.*
import com.github.kklisura.cdt.services.impl.*
import com.github.kklisura.cdt.services.invocation.*
import com.github.kklisura.cdt.services.utils.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.java_websocket.client.*
import org.java_websocket.framing.CloseFrame.*
import org.java_websocket.handshake.*
import java.lang.reflect.*
import java.net.*
import java.util.concurrent.*

private const val CAPABILITY_NAME = "debuggerAddress"
private const val DEV_TOOL_PROPERTY_NAME = "webSocketDebuggerUrl"
private const val SELENOID_WS_TIMEOUT_SEC: Long = 2
private const val RETRY_ADD_HEADERS_SLEEP_MILLIS: Long = 2000

object DevToolsClientThreadStorage {
    private val logger = Logging.logger(ChromeDevTool::class.java.name)
    private val threadLocalChromeDevTool: InheritableThreadLocal<ChromeDevTool> = InheritableThreadLocal()

    fun addHeaders(headers: Map<*, *>) {
        try {
            logger.debug { "try to add headers: $headers" }
            @Suppress("UNCHECKED_CAST")
            getDevTool()?.addHeaders(headers as Map<String, String>)
            logger.debug { "Chrome Tool activated: ${threadLocalChromeDevTool.get() != null}. Headers: $headers" }

        } catch (ex: Exception) {
            //todo refactor and remove this workaround EPMDJ-9354
            logger.debug { "try to resend because of exception: $ex" }
            Thread.sleep(RETRY_ADD_HEADERS_SLEEP_MILLIS)
            logger.debug { "[Second try] after sleep try to add headers: $headers" }
            try {
                @Suppress("UNCHECKED_CAST")
                getDevTool()?.addHeaders(headers as Map<String, String>)
                logger.debug { "[Second try] Chrome Tool activated: ${threadLocalChromeDevTool.get() != null}. Headers: $headers" }
            } catch (ex: Exception) {
                logger.warn { "cannot resend for $headers because of exception: $ex" }
                clean()
            }
        }
    }

    fun setDevTool(devTool: ChromeDevTool) {
        getDevTool()?.close()
        threadLocalChromeDevTool.set(devTool)
        logger.debug { "DevTool inited for: Thread id=${Thread.currentThread().id}, DevTool instance=$devTool" }
    }

    fun getDevTool(): ChromeDevTool? = threadLocalChromeDevTool.get()

    fun isHeadersAdded() = threadLocalChromeDevTool.get()?.isHeadersAdded ?: false

    fun resetHeaders() = runCatching { getDevTool()?.addHeaders(emptyMap()) }.onFailure {
        logger.warn { "Cannot reset headers because of exception: ${it.message}" }
    }.getOrNull()

    fun clean() {
        getDevTool()?.close()
        threadLocalChromeDevTool.remove()
    }
}

/**
 * Works with local or Selenoid DevTools by websocket
 */
class ChromeDevTool {
    private val logger = Logging.logger(ChromeDevTool::class.java.name)
    private var localDevToolsWs: ChromeDevToolWs? = null
    private var selenoidDevToolsWs: ChromeDevToolsService? = null
    lateinit var localUrl: String

    internal var isHeadersAdded: Boolean = false

    init {
        DevToolsClientThreadStorage.setDevTool(this)
    }

    fun addHeaders(headers: Map<String, String>) {
        trackTime("send headers") {
            localDevToolsWs?.addHeaders(headers)
            selenoidDevToolsWs?.network?.let {
                it.setExtraHTTPHeaders(headers)
                it.enable()
            }
        }
    }

    /**
     * connect to remote Selenoid or local webDriver
     */
    fun connect(capabilities: Map<*, *>?, sessionId: String?, remoteHost: String?) = kotlin.runCatching {
        logger.debug { "starting connectToDevTools with cap='$capabilities' sessionId='$sessionId' remote='$remoteHost'..." }
        trackTime("connect to selenoid") {
            remoteHost?.connectToSelenoid(sessionId)
        }
        if (selenoidDevToolsWs == null || selenoidDevToolsWs?.isClosed == true) {
            trackTime("connect to local") {
                connectToLocal(capabilities)
            }
        }
    }.getOrNull()

    private fun connectToLocal(capabilities: Map<*, *>?) {
        capabilities?.let { cap ->
            val debuggerURL = cap[CAPABILITY_NAME].toString()
            val con = doDevToolsRequest(debuggerURL)
            val responseCode = con.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = con.inputStream.reader().readText()
                logger.debug { "Chrome info: $response" }
                val chromeInfo = Json.parseToJsonElement(response) as JsonObject
                chromeInfo[DEV_TOOL_PROPERTY_NAME]?.jsonPrimitive?.contentOrNull?.let { url ->
                    this.localUrl = url
                    connectWs()
                } ?: logger.warn { "Can't get DevTools URL" }
            } else {
                logger.warn { "Can't get chrome info: code=$responseCode" }
            }
        }
    }

    private fun String.connectToSelenoid(sessionId: String?) {
        logger.debug { "connect to selenoid by ws..." }
        val webSocketService = WebSocketServiceImpl.create(URI("ws://$this/devtools/$sessionId/page"))
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

    fun close() {
        localDevToolsWs?.let {
            logger.debug { "${this.localUrl} closing..." }
            it.close()
        }
        selenoidDevToolsWs?.let {
            logger.debug { "closing Selenoid ws..." }
            it.close()
        }
    }

    internal fun connectWs(): Boolean {
        logger.debug { "DevTools URL: ${this.localUrl}" }
        val cdl = CountDownLatch(4)
        localDevToolsWs = ChromeDevToolWs(URI(this.localUrl), cdl, this)
        localDevToolsWs?.connect()
        return cdl.await(5, TimeUnit.SECONDS)
    }

    private fun doDevToolsRequest(debuggerURL: String): HttpURLConnection {
        val obj = URL("http://$debuggerURL/json/version")
        logger.debug { "Try to get chrome info [${obj.path}]" }
        val con = obj.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        return con
    }
}

/**
 * WS for local DevTools
 */
class ChromeDevToolWs(
    private val url: URI,
    private val cdl: CountDownLatch,
    private val chromeDevTool: ChromeDevTool,
) : WebSocketClient(url) {

    private val json = Json { ignoreUnknownKeys = true }

    private val logger = Logging.logger(ChromeDevToolWs::class.java.name)

    private lateinit var sessionId: SessionId

    override fun onOpen(handshakedata: ServerHandshake?) {
        logger.trace { "Ws was opened" }
        send(DevToolsRequest(1, "Target.getTargets", emptyMap()))
    }

    override fun onMessage(message: String) = runCatching {
        logger.trace { message }
        val parsedMessage = json.parseToJsonElement(message) as JsonObject
        val id = parsedMessage["id"] ?: return@runCatching
        val result = (parsedMessage["result"] as JsonObject).toString()
        val step = (id as JsonPrimitive).int
        when (step) {
            1 -> sendCreateSessionRequest(result)
            2 -> sendAttachRequest(result)
            3 -> sendLogClearRequest()
            4 -> sendEnableNetworkRequest()
            else -> logger.debug { "unprocessed step: $step" }
        }
    }.getOrElse { logger.error(it) { "skipped message: $message" } }


    fun addHeaders(headers: Map<String, String>) {
        sendHeadRequest(mapOf("headers" to headers))
    }

    private fun sendHeadRequest(params: Map<String, Map<String, String>>) {
        send(DevToolsHeaderRequest(6, "Network.setExtraHTTPHeaders", params, sessionId.sessionId))
    }

    private fun sendEnableNetworkRequest() {
        send(DevToolsRequest(5, "Network.enable", emptyMap(), sessionId.sessionId))
        cdl.countDown()
    }

    private fun sendLogClearRequest() {
        send(DevToolsRequest(4, "Log.clear", emptyMap(), sessionId.sessionId))
        cdl.countDown()
    }

    private fun sendAttachRequest(result: String) {
        logger.debug { "DevTools session created" }
        sessionId = json.decodeFromString(SessionId.serializer(), result)
        val params = mapOf("autoAttach" to true, "waitForDebuggerOnStart" to false).toOutput()
        send(DevToolsRequest(3, "Target.setAutoAttach", params, sessionId.sessionId))
        cdl.countDown()
    }

    private fun sendCreateSessionRequest(result: String) {
        val targetId = retrieveTargetId(result)
        val params = mapOf("targetId" to targetId, "flatten" to true).toOutput()
        send(DevToolsRequest(2, "Target.attachToTarget", params))
        cdl.countDown()
    }

    private fun retrieveTargetId(result: String) = json.decodeFromString(TargetInfos.serializer(), result).targetInfos
        .filter { it.type == "page" }
        .map { it.targetId }
        .first()
        .toUpperCase()


    override fun onError(ex: java.lang.Exception?) {
        logger.error(ex) { "socket ${this.url} closed by error:" }

    }

    private fun send(value: DevToolsRequest) {
        if (isOpen) {
            send(json.encodeToString(DevToolsRequest.serializer(), value))
        }
    }

    private fun send(value: DevToolsHeaderRequest) {
        if (isOpen) {
            send(json.encodeToString(DevToolsHeaderRequest.serializer(), value))
            chromeDevTool.isHeadersAdded = true
        }
    }


    override fun onClose(code: Int, reason: String?, remote: Boolean) = when (code) {
        NORMAL -> logger.debug { "socket closed. Code: $code, reason: $reason, remote: $remote" }
        else -> {
            Thread.sleep(1000)
            logger.debug { "try reconnect to ${this.url}" }.also { chromeDevTool.connectWs() }
        }
    }

}

fun Map<String, Any>.toOutput(): Map<String, JsonElement> = mapValues { (_, value) ->
    val serializer = value::class.serializer().cast()
    json.encodeToJsonElement(serializer, value)
}

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
internal inline fun <T> KSerializer<out T>.cast(): KSerializer<T> = this as KSerializer<T>


@Serializable
data class TargetInfos(val targetInfos: List<Target>)

@Serializable
data class SessionId(val sessionId: String)

@Serializable
data class Target(
    val targetId: String,
    val type: String,
    val title: String,
    val url: String,
    val attached: Boolean,
    val browserContextId: String,
)

@Serializable
data class DevToolsRequest(
    val id: Int,
    val method: String,
    val params: Map<String, JsonElement> = emptyMap(),
    val sessionId: String? = null,
)

@Serializable
data class DevToolsHeaderRequest(
    val id: Int,
    val method: String,
    val params: Map<String, Map<String, String>> = emptyMap(),
    val sessionId: String? = null,
)
