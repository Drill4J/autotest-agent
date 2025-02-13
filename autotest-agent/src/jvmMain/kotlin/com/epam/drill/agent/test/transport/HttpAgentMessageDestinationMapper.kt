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
package com.epam.drill.agent.test.transport

import com.epam.drill.agent.transport.AgentMessageDestinationMapper
import com.epam.drill.agent.common.transport.AgentMessageDestination

class HttpAgentMessageDestinationMapper(private val apiPath: String) : AgentMessageDestinationMapper {

    override fun map(destination: AgentMessageDestination): AgentMessageDestination =
        destination.copy(
            target =
            if (destination.target.isEmpty()) apiPath else "${apiPath}/${destination.target}"
        )
}
