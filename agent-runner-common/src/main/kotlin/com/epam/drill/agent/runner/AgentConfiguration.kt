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
package com.epam.drill.agent.runner

open class AgentConfiguration : Configuration() {
    var pluginId: String = "test2code"
    var plugins: Set<String> = mutableSetOf()
    var labels: Map<String, String>? = null
    override val repositoryName: String = "autotest-agent"

    override fun jvmArgs(): Map<String, String> {
        val args = mutableMapOf<String, String>()
        args[AgentConfiguration::pluginId.name] = pluginId
        args[AgentConfiguration::plugins.name] = plugins.joinToString(separator = ";")
        labels?.let {
            args[AgentConfiguration::labels.name] = it.map { (k, v) -> "$k:$v" }.joinToString(";")
        }
        return args
    }

}
