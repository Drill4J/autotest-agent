package com.epam.drill.test.agent

expect object AgentConfig {
    fun proxyUrl(): String?
    fun dispatcherUrl(): String?
    fun adminAddress(): String?
    fun agentId(): String?
    fun groupId(): String?
    fun agentUrl(): String?
    fun extensionUrl(): String?
    fun extensionVersion(): String?
}
