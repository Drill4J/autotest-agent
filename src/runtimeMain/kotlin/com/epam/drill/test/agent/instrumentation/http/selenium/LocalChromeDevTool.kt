package com.epam.drill.test.agent.instrumentation.http.selenium

import com.epam.drill.logger.*
import com.epam.drill.test.agent.config.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.java_websocket.client.*
import org.java_websocket.framing.*
import org.java_websocket.handshake.*
import java.net.*
import java.util.concurrent.*

private const val CAPABILITY_NAME = "debuggerAddress"
private const val DEV_TOOL_PROPERTY_NAME = "webSocketDebuggerUrl"

class LocalChromeDevTool {
    private val logger = Logging.logger(LocalChromeDevTool::class.java.name)
    private var devToolsWs: LocalChromeDevToolWs? = null
    lateinit var url: String
    internal var isHeadersAdded: Boolean = false


    fun addHeaders(headers: Map<String, String>) {
        devToolsWs?.addHeaders(headers)
    }

    fun connect(capabilities: Map<*, *>?) {
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
                    connectWs()
                } ?: logger.warn { "Can't get DevTools URL" }
            } else {
                logger.warn { "Can't get chrome info: code=$responseCode" }
            }
        }
    }

    fun connectWs(): Boolean {
        logger.debug { "DevTools URL: ${this.url}" }
        val cdl = CountDownLatch(4)
        devToolsWs = LocalChromeDevToolWs(URI(this.url), cdl, this)
        devToolsWs?.connect()
        return cdl.await(5, TimeUnit.SECONDS)
    }

    private fun doDevToolsRequest(debuggerURL: String): HttpURLConnection {
        val obj = URL("http://$debuggerURL/json/version")
        logger.debug { "Try to get chrome info [${obj.path}]" }
        val con = obj.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        return con
    }

    fun close() {
        devToolsWs?.let {
            logger.debug { "${this.url} closing..." }
            it.close()
        }
    }
}


class LocalChromeDevToolWs(
    private val url: URI,
    private val cdl: CountDownLatch,
    private val chromeDevTool: LocalChromeDevTool,
) : WebSocketClient(url) {

    private val json = Json { ignoreUnknownKeys = true }

    private val logger = Logging.logger(LocalChromeDevToolWs::class.java.name)

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
        CloseFrame.NORMAL -> logger.debug { "socket closed. Code: $code, reason: $reason, remote: $remote" }
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
