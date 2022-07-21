package com.epam.drill.test.agent

import com.epam.drill.test.agent.config.*
import kotlin.native.concurrent.*

actual object AgentConfig {
    private val _config = AtomicReference(AgentRawConfig().freeze()).freeze()
    val config
        get() = _config.value

    actual fun proxyUrl(): String? {
        return config.browserProxyAddress
    }

    actual fun adminAddress(): String? = config.adminAddress

    actual fun agentId(): String? = config.agentId

    actual fun groupId(): String? = config.groupId

    actual fun devToolsProxyAddress(): String? = config.devToolsProxyAddress

    actual fun withJsCoverage(): Boolean = config.withJsCoverage

    actual fun launchType(): String? = config.launchType

    fun updateConfig(config: AgentRawConfig) {
        _config.value = config
    }
}
