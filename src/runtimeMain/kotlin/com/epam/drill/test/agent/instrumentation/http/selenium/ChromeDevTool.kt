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
import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.config.*
import com.epam.drill.test.agent.http.*
import com.epam.drill.test.agent.util.*
import kotlinx.atomicfu.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.net.*
import java.util.*

private const val DEBUGGER_ADDRESS = "debuggerAddress"
private const val DEV_TOOL_DEBUGGER_URL = "webSocketDebuggerUrl"
private val JAVA_TOGGLES = listOf("Network")
private val JS_TOGGLES = listOf("Debugger", "Profiler").takeIf { AgentConfig.withJsCoverage() } ?: emptyList()

object DevToolStorage {
    private val logger = Logging.logger(DevToolStorage::class.java.name)
    private val storage: InheritableThreadLocal<ChromeDevTool> = InheritableThreadLocal()

    fun set(devtool: ChromeDevTool) {
        storage.set(devtool)
        logger.debug { "DevTool inited for: Thread id=${Thread.currentThread().id}, DevToolWS address=$devtool" }
    }

    fun get(): ChromeDevTool? = storage.get()

    fun clear() = storage.remove()
}

/**
 * Works with local or Selenoid DevTools by websocket
 */
class ChromeDevTool(
    private val capabilities: Map<*, *>?,
    private val remoteHost: String?
) {
    private val logger = Logging.logger(ChromeDevTool::class.java.name)
    private val devToolsProxyAddress = AgentConfig.devToolsProxyAddress()?.let {
        if (it.startsWith("http")) it else "http://$it"
    }
    private val isClosed = atomic(false)

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private lateinit var targetUrl: String
    private lateinit var targetId: String
    private var sessionId: SessionId = SessionId()
    private var headersAdded: Boolean = false

    /**
     * connect to remote Selenoid or local webDriver
     */
    fun connect(browserSessionId: String?, currentUrl: String) = runCatching {
        logger.debug { "starting connectToDevTools with cap='$capabilities' sessionId='$sessionId' remote='$remoteHost'..." }
        retrieveDevToolAddress(capabilities, browserSessionId, remoteHost)?.let {
            trackTime("connect to devtools") { connect(it, currentUrl) }
        }
        /**
         * Add this to thread local only if successfully connected
         */
    }.onFailure { logger.warn(it) { "UI coverage will be lost. Reason: " } }.getOrNull()

    fun addHeaders(headers: Map<*, *>) {
        @Suppress("UNCHECKED_CAST")
        val casted = headers as Map<String, String>
        try {
            logger.debug { "try to add headers: $headers" }
            val success = setHeaders(casted)
            logger.debug { "Chrome Tool activated: ${sessionId.sessionId.isNotBlank()}. Headers: $headers" }
            if (!success) throw RuntimeException("Can't add headers: $headers")
        } catch (ex: Exception) {
            logger.debug { "exception $ex; try to resend" }
            Thread.sleep(2000)
            setHeaders(casted)
        }
    }

    fun resetHeaders() = setHeaders(emptyMap())

    fun switchSession(url: String) {
        val targetId = retrieveTargetId(url)
        logger.trace { "Reconnect to target: $targetId, sessionId: ${sessionId.sessionId}, url $url" }
        targetId?.takeIf { it != this.targetId }?.let { attachToTarget(it) }?.let {
            this.targetId = targetId
            sessionId = it
            enableToggles()
            startCollectJsCoverage()
        }
    }

    fun isHeadersAdded(): Boolean = this.headersAdded

    private fun startCollectJsCoverage() = AgentConfig.takeIf { it.withJsCoverage() }?.let {
        disableCache() && startPreciseCoverage() && enableScriptParsed()
    }?.also { success ->
        if (!success) logger.warn { "JS coverage may be lost" }
    }

    private fun startPreciseCoverage() = mapOf("detailed" to true, "callCount" to false).let { params ->
        executeCommand(
            "Profiler.startPreciseCoverage",
            DevToolsRequest(targetUrl, sessionId.sessionId, params.toOutput())
        ).code == HttpURLConnection.HTTP_OK
    }

    private fun disableCache() = executeCommand(
        "Network.setCacheDisabled",
        DevToolsRequest(targetUrl, sessionId.sessionId, mapOf("cacheDisabled" to true).toOutput())
    ).code == HttpURLConnection.HTTP_OK

    private fun enableScriptParsed() = HttpClient.request("$devToolsProxyAddress/event/Debugger.scriptParsed") {
        body = json.encodeToString(DevToolsMessage.serializer(), DevToolsRequest(targetUrl, sessionId.sessionId))
    }.code == HttpURLConnection.HTTP_OK

    fun takePreciseCoverage(): String = executeCommand(
        "Profiler.takePreciseCoverage",
        DevToolsRequest(targetUrl, sessionId.sessionId)
    ).takeIf { it.code == HttpURLConnection.HTTP_OK }?.body ?: ""

    fun scriptParsed(): String = HttpClient.request("$devToolsProxyAddress/event/Debugger.scriptParsed/get-data") {
        body = json.encodeToString(DevToolsMessage.serializer(), DevToolsRequest(targetUrl, sessionId.sessionId))
    }.takeIf { it.code == HttpURLConnection.HTTP_OK }?.body ?: ""

    fun close() {
        if (!isClosed.value) {
            disableToggles()
            stopCollectJsCoverage()
            HttpClient.request("$devToolsProxyAddress/connection") {
                method = HttpMethod.DELETE
                body = json.encodeToString(DevToolsRequest.serializer(), DevToolsRequest(targetUrl))
            }
        }
        isClosed.update { true }
    }

    private fun stopCollectJsCoverage() = AgentConfig.takeIf { it.withJsCoverage() }?.let {
        HttpClient.request("$devToolsProxyAddress/event/Debugger.scriptParsed") {
            method = HttpMethod.DELETE
            body = json.encodeToString(
                DevToolsMessage.serializer(),
                DevToolsRequest(targetUrl, sessionId.sessionId)
            )
        }
    }

    // todo is it necessary to disable toggles when browser exit?
    private fun disableToggles() = (JAVA_TOGGLES + JS_TOGGLES).map {
        executeCommand(
            "$it.disable",
            DevToolsRequest(targetUrl, sessionId.sessionId)
        ).code == HttpURLConnection.HTTP_OK
    }.all { it }

    private fun enableToggles() = (JAVA_TOGGLES + JS_TOGGLES).map {
        executeCommand(
            "$it.enable",
            DevToolsRequest(targetUrl, sessionId.sessionId)
        ).code == HttpURLConnection.HTTP_OK
    }.all { it }.also {
        if (!it) logger.warn { "Toggles wasn't enable" } else logger.info { "Toggles enabled" }
    }

    private fun retrieveDevToolAddress(
        capabilities: Map<*, *>?,
        sessionId: String?,
        remoteHost: String?
    ): String? = takeIf { !remoteHost.isNullOrBlank() && !capabilities.isNullOrEmpty() }?.let {
        "ws://$remoteHost/devtools/$sessionId"
    } ?: capabilities?.run {
        val debuggerURL = get(DEBUGGER_ADDRESS).toString()
        val response = HttpClient.request("http://$debuggerURL/json/version")
        if (response.code == HttpURLConnection.HTTP_OK) {
            logger.debug { "Chrome info: ${response.body}" }
            val chromeInfo = Json.parseToJsonElement(response.body) as JsonObject
            chromeInfo[DEV_TOOL_DEBUGGER_URL]?.jsonPrimitive?.contentOrNull
        } else {
            logger.warn { "Can't get chrome info: code=${response.code}, body:${response.body}" }
            null
        }
    }

    private fun connect(devToolAddress: String, currentUrl: String) {
        //todo hack: browser doesn't allow connect from docker??
        targetUrl = devToolAddress.replace("localhost", "host.docker.internal")
        val success: Boolean = connectToDevTools().takeIf { it }?.also {
            val targetId = retrieveTargetId(currentUrl)
            logger.info { "Retrieved target for url $currentUrl: $targetId" }
            targetId?.let { attachToTarget(it) }?.also {
                this.targetId = targetId
                sessionId = it
                logger.debug { "DevTools session created: $sessionId" }
                enableToggles()
                startCollectJsCoverage()
            }
        } ?: false
        if (success) {
            DevToolStorage.set(this)
        } else throw RuntimeException("Can't connect to $targetUrl")
    }

    private fun connectToDevTools(): Boolean {
        logger.debug { "DevTools URL: $targetUrl" }
        val response = HttpClient.request("$devToolsProxyAddress/connection") {
            method = HttpMethod.POST
            body = json.encodeToString(DevToolsRequest.serializer(), DevToolsRequest(targetUrl))
        }
        return response.code == HttpURLConnection.HTTP_OK
    }

    fun startIntercept(): Boolean = ThreadStorage.storage.get()?.let { testHash ->
        val headers = mapOf(
            TEST_ID_HEADER to testHash,
            SESSION_ID_HEADER to (ThreadStorage.sessionId() ?: "")
        )
        logger.debug { "Start intercepting. Headers: $headers, sessionId: $sessionId" }
        val response = HttpClient.request("$devToolsProxyAddress/intercept") {
            method = HttpMethod.POST
            body = json.encodeToString(
                DevToolInterceptRequest.serializer(),
                DevToolInterceptRequest(targetUrl, params = mapOf("headers" to headers))
            )
        }
        response.code == HttpURLConnection.HTTP_OK
    } ?: false

    fun stopIntercept(): Boolean {
        logger.debug { "Stop intercepting: $targetUrl, sessionId $sessionId" }
        val response = HttpClient.request("$devToolsProxyAddress/intercept") {
            method = HttpMethod.DELETE
            body = json.encodeToString(DevToolInterceptRequest.serializer(), DevToolInterceptRequest(targetUrl))
        }
        setHeaders(mapOf())
        return response.code == HttpURLConnection.HTTP_OK
    }

    private fun setHeaders(
        params: Map<String, String>
    ): Boolean = executeCommand(
        "Network.setExtraHTTPHeaders",
        DevToolsHeaderRequest(targetUrl, sessionId.sessionId, mapOf("headers" to params))
    ).code == HttpURLConnection.HTTP_OK

    @Deprecated(message = "Useless")
    private fun autoAttach(): Boolean {
        val params = mapOf("autoAttach" to true, "waitForDebuggerOnStart" to false).toOutput()
        return executeCommand(
            "Target.setAutoAttach",
            DevToolsRequest(targetUrl, sessionId.sessionId, params = params)
        ).code == HttpURLConnection.HTTP_OK
    }

    private fun attachToTarget(targetId: String): SessionId? {
        val params = mapOf("targetId" to targetId, "flatten" to true).toOutput()
        val response = executeCommand(
            "Target.attachToTarget",
            DevToolsRequest(target = targetUrl, params = params)
        )
        return response.takeIf { it.code == HttpURLConnection.HTTP_OK }?.let {
            json.decodeFromString(SessionId.serializer(), it.body)
        }
    }

    private fun retrieveTargetId(currentUrl: String): String? = targets()
        .find { it.url == currentUrl }
        ?.targetId
        ?.uppercase(Locale.getDefault())
        ?.takeIf { it.isNotBlank() }

    private fun targets(): List<Target> = executeCommand(
        "Target.getTargets",
        DevToolsRequest(target = targetUrl)
    ).takeIf { it.code == HttpURLConnection.HTTP_OK }?.let { response ->
        json.decodeFromString(TargetInfos.serializer(), response.body).targetInfos
    } ?: emptyList()

    private fun executeCommand(
        commandName: String,
        request: DevToolsMessage,
        httpMethod: HttpMethod = HttpMethod.POST,
    ) = HttpClient.request("$devToolsProxyAddress/command/$commandName") {
        method = httpMethod
        timeout = 15_000
        body = json.encodeToString(DevToolsMessage.serializer(), request)
    }
}

fun Map<String, Any>.toOutput(): Map<String, JsonElement> = mapValues { (_, value) ->
    val serializer = value::class.serializer().cast()
    json.encodeToJsonElement(serializer, value)
}

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
internal inline fun <T> KSerializer<out T>.cast(): KSerializer<T> = this as KSerializer<T>
