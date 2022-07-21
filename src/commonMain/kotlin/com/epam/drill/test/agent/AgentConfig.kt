package com.epam.drill.test.agent

expect object AgentConfig {
    fun proxyUrl(): String?
    fun adminAddress(): String?
    fun agentId(): String?
    fun groupId(): String?
    fun devToolsProxyAddress(): String?
    fun withJsCoverage(): Boolean
    fun launchType(): String?
}
