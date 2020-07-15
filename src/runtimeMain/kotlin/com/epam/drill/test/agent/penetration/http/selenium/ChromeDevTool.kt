package com.epam.drill.test.agent.penetration.http.selenium

import com.epam.drill.test.agent.penetration.http.selenium.DevToolsClientThreadStorage.crhmT
import com.epam.drill.logger.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.java_websocket.client.*
import org.java_websocket.handshake.*
import java.net.*
import java.util.concurrent.*

private const val CAPABILITY_NAME = "debuggerAddress"
private const val DEV_TOOL_PROPERTY_NAME = "webSocketDebuggerUrl"

object DevToolsClientThreadStorage {
    internal var crhmT: InheritableThreadLocal<ChromeDevTool> = InheritableThreadLocal()

    fun addHeaders(headers: Map<*, *>) {
        @Suppress("UNCHECKED_CAST")
        crhmT.get()?.addHeaders(headers as Map<String, String>)
    }
}

class ChromeDevTool {
    private val logger = Logging.logger(ChromeDevTool::class.java.name)

    private var ws: ChromeDevToolWs? = null

    fun addHeaders(headers: Map<String, String>) {
        ws?.addHeaders(headers)
    }

    init {
        crhmT.set(this)
    }


    fun connectToDevTools(capabilities: Map<*, *>?) = kotlin.runCatching {
        capabilities?.let { cap ->
            val debuggerURL = cap[CAPABILITY_NAME].toString()
            val con = doDevToolsRequest(debuggerURL)
            val responseCode = con.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {

                val response = con.inputStream.reader().readText()
                logger.debug { "Chrome info: $response" }
                val chromeInfo = Json.parseJson(response) as JsonObject
                chromeInfo[DEV_TOOL_PROPERTY_NAME]?.content?.let { url ->
                    logger.debug { "DevTools URL: $url" }
                    val cdl = CountDownLatch(4)
                    ws = ChromeDevToolWs(URI(url), cdl)
                    ws?.connect()
                    cdl.await(5, TimeUnit.SECONDS)
                } ?: logger.warn { "Can't get DevTools URL" }
            } else {
                logger.warn { "Can't get chrome info: code=$responseCode" }
            }
        }
    }.getOrNull()

    private fun doDevToolsRequest(debuggerURL: String): HttpURLConnection {
        val obj = URL("http://$debuggerURL/json/version")
        logger.debug { "Try to get chrome info [${obj.path}]" }
        val con = obj.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        return con
    }
}

class ChromeDevToolWs(uri: URI, private val cdl: CountDownLatch) : WebSocketClient(uri) {
    private val json = Json(JsonConfiguration(encodeDefaults = false))

    private val logger = Logging.logger(ChromeDevToolWs::class.java.name)

    private lateinit var sessionId: SessionId

    override fun onOpen(handshakedata: ServerHandshake?) {
        send(DevToolsRequest(1, "Target.getTargets", emptyMap()))
    }

    override fun onMessage(message: String) = runCatching {
        logger.trace { message }
        val parsedMessage = json.parseJson(message) as JsonObject
        val id = parsedMessage["id"] ?: return@runCatching
        val result = (parsedMessage["result"] as JsonObject).toString()
        val step = (id as JsonLiteral).int
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
        sessionId = json.parse(SessionId.serializer(), result)
        val params = mapOf("autoAttach" to true, "waitForDebuggerOnStart" to false)
        send(DevToolsRequest(3, "Target.setAutoAttach", params, sessionId.sessionId))
        cdl.countDown()
    }

    private fun sendCreateSessionRequest(result: String) {
        val targetId = retrieveTargetId(result)
        val params = mapOf("targetId" to targetId, "flatten" to true)
        send(DevToolsRequest(2, "Target.attachToTarget", params))
        cdl.countDown()
    }

    private fun retrieveTargetId(result: String) = json.parse(TargetInfos.serializer(), result).targetInfos
        .filter { it.type == "page" }
        .map { it.targetId }
        .first()
        .toUpperCase()


    override fun onError(ex: java.lang.Exception?) {
        logger.error(ex) { "socket closed by error:" }
    }

    private fun send(value: DevToolsRequest) {
        send(json.stringify(DevToolsRequest.serializer(), value))
    }

    private fun send(value: DevToolsHeaderRequest) {
        send(json.stringify(DevToolsHeaderRequest.serializer(), value))
    }


    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        logger.debug { "socket closed" }
    }

}

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
    val params: Map<String, @ContextualSerialization Any> = emptyMap(),
    val sessionId: String? = null
)

@Serializable
data class DevToolsHeaderRequest(
    val id: Int,
    val method: String,
    val params: Map<String, Map<String, String>> = emptyMap(),
    val sessionId: String? = null
)
