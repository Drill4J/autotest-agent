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

import com.epam.drill.common.agent.configuration.AgentParameterDefinition
import com.epam.drill.plugins.test2code.api.Label

object ParameterDefinitions {

    val ADMIN_ADDRESS = AgentParameterDefinition.forString(
        name = "adminAddress",
        parser = { it.takeIf(URL_SCHEME_REGEX::matches) ?: "http://$it"}
    )
    val API_KEY = AgentParameterDefinition.forString(name = "apiKey")
    val MESSAGE_QUEUE_LIMIT = AgentParameterDefinition.forString(name = "coverageRetentionLimit", defaultValue = "512Mb")
    val SSL_TRUSTSTORE = AgentParameterDefinition.forString(name = "sslTruststore")
    val SSL_TRUSTSTORE_PASSWORD = AgentParameterDefinition.forString(name = "sslTruststorePassword")
    val LOG_LEVEL = AgentParameterDefinition.forString(name = "logLevel", defaultValue = "INFO")
    val LOG_FILE = AgentParameterDefinition.forString(name = "logFile")
    val LOG_LIMIT = AgentParameterDefinition.forInt(name = "logLimit", defaultValue = 512)
    val PLUGIN_ID = AgentParameterDefinition.forString(name = "pluginId", defaultValue = "test2code")
    val IS_REALTIME_ENABLED = AgentParameterDefinition.forBoolean(name = "isRealtimeEnable")
    val IS_GLOBAL = AgentParameterDefinition.forBoolean(name = "isGlobal")
    val IS_MANUALLY_CONTROLLED = AgentParameterDefinition.forBoolean(name = "isManuallyControlled")
    val SESSION_FOR_EACH_TEST = AgentParameterDefinition.forBoolean(name = "sessionForEachTest")
    val WITH_JS_COVERAGE = AgentParameterDefinition.forBoolean(name = "withJsCoverage")
    val PROXY_ADDRESS = AgentParameterDefinition.forString(name = "browserProxyAddress")
    val DEVTOOLS_PROXY_ADDRESS = AgentParameterDefinition.forString(name = "devToolsProxyAddress")
    val DEVTOOLS_REPLACE_LOCALHOST = AgentParameterDefinition.forString(name = "devtoolsAddressReplaceLocalhost")
    val SESSION_ID = AgentParameterDefinition.forString(name = "sessionId")
    val LAUNCH_TYPE = AgentParameterDefinition.forString(name = "launchType")
    val LABELS = AgentParameterDefinition.forType(
        name = "labels",
        defaultValue = emptySet(),
        parser = {
            it.takeIf(String::isNotBlank)?.split(";")
                ?.map { Label(it.substringBefore(":"), it.substringAfter(":", "")) }
                ?.toSet()
                ?: emptySet()
        }
    )
    val FRAMEWORK_PLUGINS = AgentParameterDefinition.forType(
        name = "rawFrameworkPlugins",
        defaultValue = emptyList(),
        parser = { it.split(";") }
    )

    private val URL_SCHEME_REGEX = Regex("\\w+://.+")

}
