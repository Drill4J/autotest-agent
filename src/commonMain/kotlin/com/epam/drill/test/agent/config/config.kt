package com.epam.drill.test.agent.config

import com.epam.drill.logger.api.*
import kotlinx.serialization.*

@Serializable
data class AgentRawConfig(
    val agentId: String = "",
    val groupId: String = "",
    val pluginId: String = "",
    val adminAddress: String = "",
    val drillInstallationDir: String = "",
    val logFile: String? = null,
    val logLevel: String = LogLevel.ERROR.name,
    val rawFrameworkPlugins: String = "",
    val sessionId: String? = null,
    val isRealtimeEnable: Boolean = true,
    val isGlobal: Boolean = false,
    val browserProxyAddress: String? = null,
    val isManuallyControlled: Boolean = false,
    val sessionForEachTest: Boolean = false
) {
    val level: LogLevel
        get() = LogLevel.valueOf(logLevel)

    val frameworkPlugins: List<String>
        get() = rawFrameworkPlugins.split(";")
}
