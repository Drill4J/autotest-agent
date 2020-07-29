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
    val browserProxyAddress: String? = null
) {
    val level: LogLevel
        get() = LogLevel.valueOf(logLevel)

    val frameworkPlugins: List<String>
        get() = rawFrameworkPlugins.split(";")
    val adminHost: String
        get() {
            val url = adminAddress.split(":")
            return if (url.size > 1)
                url[0]
            else adminAddress
        }
    val adminPort: String
        get() {
            val url = adminAddress.split(":")
            return if (url.size > 1)
                url[1]
            else "80"
        }
}
