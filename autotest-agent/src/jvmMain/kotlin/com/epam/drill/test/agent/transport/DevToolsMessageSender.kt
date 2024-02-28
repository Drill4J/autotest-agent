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

import kotlinx.serialization.DeserializationStrategy
import com.epam.drill.agent.transport.JsonAgentMessageSerializer
import com.epam.drill.agent.transport.http.HttpAgentMessageTransport
import com.epam.drill.agent.transport.http.HttpResponseContent
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.test.agent.configuration.Configuration
import com.epam.drill.test.agent.configuration.ParameterDefinitions
import com.epam.drill.test.agent.instrument.strategy.selenium.DevToolsMessage

object DevToolsMessageSender {

    private val messageTransport = HttpAgentMessageTransport(
        serverAddress = Configuration.parameters[ParameterDefinitions.DEVTOOLS_PROXY_ADDRESS],
        drillInternal = false,
        gzipCompression = false,
        receiveContent = true
    )
    private val messageSerializer = JsonAgentMessageSerializer()
    private val messageDeserializer = JsonAgentMessageDeserializer()

    @Suppress("unchecked_cast")
    fun send(
        destination: AgentMessageDestination,
        message: DevToolsMessage
    ) = messageTransport.send(destination, messageSerializer.serialize(message), messageSerializer.contentType())
        .let { it as HttpResponseContent<ByteArray> }
        .let { HttpResponseContent(it.statusObject, it.content.decodeToString()) }

    @Suppress("unchecked_cast")
    fun <T : AgentMessage> send(
        destination: AgentMessageDestination,
        message: DevToolsMessage,
        strategy: DeserializationStrategy<T>
    ) = messageTransport.send(destination, messageSerializer.serialize(message), messageSerializer.contentType())
        .let { it as HttpResponseContent<ByteArray> }
        .let { HttpResponseContent(it.statusObject, messageDeserializer.deserialize(strategy, it.content)) }

    @Suppress("unchecked_cast")
    fun send(
        serverAddress: String,
        destination: AgentMessageDestination,
        message: String
    ) = HttpAgentMessageTransport(
        serverAddress = serverAddress,
        drillInternal = false,
        gzipCompression = false,
        receiveContent = true
    ).send(destination, message.encodeToByteArray(), messageSerializer.contentType())
        .let { it as HttpResponseContent<ByteArray> }
        .let { HttpResponseContent(it.statusObject, it.content.decodeToString()) }

}
