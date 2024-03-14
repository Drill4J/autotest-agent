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
package com.epam.drill.test.agent.transport

import com.epam.drill.agent.transport.AgentMessageDestinationMapper
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.test.agent.configuration.Configuration
import com.epam.drill.test.agent.configuration.ParameterDefinitions

class HttpAgentMessageDestinationMapper : AgentMessageDestinationMapper {

    private val defaultTargetPrefix = defaultTargetPrefix()

    override fun map(destination: AgentMessageDestination): AgentMessageDestination =
        destination.copy(target = "$defaultTargetPrefix/${destination.target}")

    private fun defaultTargetPrefix(): String {
        val agentId = Configuration.agentMetadata.id
        val groupId = Configuration.agentMetadata.serviceGroupId
        val pluginId = Configuration.parameters[ParameterDefinitions.PLUGIN_ID]
        if (groupId.isNotBlank())
            return "api/groups/$groupId/plugins/$pluginId"
        else
            return "api/agents/$agentId/plugins/$pluginId"
    }

}
