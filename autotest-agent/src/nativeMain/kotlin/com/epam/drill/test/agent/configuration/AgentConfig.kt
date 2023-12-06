/**
 * Copyright 2020 - 2022 EPAM Systems
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
package com.epam.drill.test.agent.configuration

import kotlin.native.concurrent.*

actual object AgentConfig {
    private val _config = AtomicReference(AgentRawConfig().freeze()).freeze()
    val config
        get() = _config.value

    actual fun proxyUrl(): String? {
        return config.browserProxyAddress
    }

    actual fun adminAddress(): String? = config.adminAddress

    actual fun adminUserName(): String? = config.adminUserName

    actual fun adminPassword(): String? = config.adminPassword

    actual fun agentId(): String? = config.agentId

    actual fun groupId(): String? = config.groupId

    actual fun devToolsProxyAddress(): String? = config.devToolsProxyAddress

    actual fun withJsCoverage(): Boolean = config.withJsCoverage

    actual fun launchType(): String? = config.launchType

    actual fun devtoolsAddressReplaceLocalhost(): String? = config.devtoolsAddressReplaceLocalhost

    fun updateConfig(config: AgentRawConfig) {
        _config.value = config
    }
}
