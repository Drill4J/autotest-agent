package com.epam.drill.test.agent

import com.epam.drill.test.agent.config.*
import kotlin.native.concurrent.*

actual object AgentConfig {
    val _config = AtomicReference(AgentRawConfig().freeze()).freeze()
    val config
        get() = _config.value

    actual fun proxyUrl(): String? {
        return config.browserProxyAddress
    }

    actual fun adminAddress(): String? = config.adminAddress

    actual fun agentId(): String? = config.agentId

    actual fun groupId(): String? = config.groupId

    actual fun dispatcherUrl(): String? = config.dispatcherUrl

    actual fun agentUrl(): String? = config.agentUrl

    actual fun extensionUrl(): String? = config.extensionUrl
    actual fun extensionVersion(): String? = config.extensionVersion
}
