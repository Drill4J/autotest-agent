package com.epam.drill.test.agent.instrumentation.http.selenium

import com.epam.drill.logger.*
import com.epam.drill.test.agent.config.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.java_websocket.client.*
import org.java_websocket.framing.CloseFrame.*
import org.java_websocket.handshake.*
import java.net.*
import java.util.concurrent.*

private const val CAPABILITY_NAME = "debuggerAddress"
private const val DEV_TOOL_PROPERTY_NAME = "webSocketDebuggerUrl"

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
            logger.debug { "exception $ex; try to resend" }
            Thread.sleep(2000)
            @Suppress("UNCHECKED_CAST")
            getDevTool()?.addHeaders(headers as Map<String, String>)
        }
    }

    fun setDevTool(devTool: ChromeDevTool) = threadLocalChromeDevTool.set(devTool).also {
        logger.debug { "Devtool inited for: Thread id=${Thread.currentThread().id}, Devtool instance=$devTool" }
    }

    fun getDevTool(): ChromeDevTool? = threadLocalChromeDevTool.get()

    fun isHeadersAdded() = threadLocalChromeDevTool.get()?.isHeadersAdded ?: false

}

class ChromeDevTool {
    private val logger = Logging.logger(ChromeDevTool::class.java.name)
    internal var ws: ChromeDevToolWs? = null
    internal var isHeadersAdded: Boolean = false

    fun addHeaders(headers: Map<String, String>) {
        ws?.addHeaders(headers)
    }

    init {
        DevToolsClientThreadStorage.setDevTool(this)
    }

    lateinit var url: String

    fun connectToDevTools(capabilities: Map<*, *>?) = kotlin.runCatching {
        capabilities?.let { cap ->
            val debuggerURL = cap[CAPABILITY_NAME].toString()
            val con = doDevToolsRequest(debuggerURL)
            val responseCode = con.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {

                val response = con.inputStream.reader().readText()
                logger.debug { "Chrome info: $response" }
                val chromeInfo = Json.parseToJsonElement(response) as JsonObject
                chromeInfo[DEV_TOOL_PROPERTY_NAME]?.jsonPrimitive?.contentOrNull?.let { url ->
                    this.url = url
                    connect()
                } ?: logger.warn { "Can't get DevTools URL" }
            } else {
                logger.warn { "Can't get chrome info: code=$responseCode" }
            }
        }
    }.getOrNull()

    fun close() {
        logger.debug { "${this.url} closing..." }
        ws?.close()
    }

    internal fun connect(): Boolean {
        logger.debug { "DevTools URL: ${this.url}" }
        val cdl = CountDownLatch(4)
        ws = ChromeDevToolWs(URI(this.url), cdl, this)
        ws?.connect()
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

class ChromeDevToolWs(
    val url: URI,
    private val cdl: CountDownLatch,
    val chromeDevTool: ChromeDevTool
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
            logger.debug { "try reconnect to ${this.url}" }.also { chromeDevTool.connect() }
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
    val browserContextId: String
)

@Serializable
data class DevToolsRequest(
    val id: Int,
    val method: String,
    val params: Map<String, JsonElement> = emptyMap(),
    val sessionId: String? = null
)

@Serializable
data class DevToolsHeaderRequest(
    val id: Int,
    val method: String,
    val params: Map<String, Map<String, String>> = emptyMap(),
    val sessionId: String? = null
)
