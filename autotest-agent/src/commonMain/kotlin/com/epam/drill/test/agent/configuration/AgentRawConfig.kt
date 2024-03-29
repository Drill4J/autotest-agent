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

import com.epam.drill.plugins.test2code.api.*
import kotlinx.serialization.*

@Serializable
data class AgentRawConfig(
    val agentId: String = "",
    val groupId: String = "",
    val pluginId: String = "test2code",
    val adminAddress: String = "",
    val adminUserName: String = "",
    val adminPassword: String = "",
    val drillInstallationDir: String = "",
    val logFile: String? = null,
    val logLevel: String = "ERROR",
    val logLimit: Int = 512,
    val rawFrameworkPlugins: String = "",
    val labels: String = "",
    val sessionId: String? = null,
    val isRealtimeEnable: Boolean = true,
    val isGlobal: Boolean = false,
    val browserProxyAddress: String? = null,
    val isManuallyControlled: Boolean = false,
    val sessionForEachTest: Boolean = false,
    val devToolsProxyAddress: String? = null,
    val withJsCoverage: Boolean = false,
    val launchType: String? = null,
    val devtoolsAddressReplaceLocalhost: String? = null,
) {

    val labelCollection
        get() = labels.takeIf { it.isNotBlank() }?.split(";")?.map {
            val (name, value) = it.split(":")
            Label(name, value)
        }?.toSet() ?: emptySet()

    val frameworkPlugins: List<String>
        get() = rawFrameworkPlugins.split(";")
}
