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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import com.epam.drill.agent.common.transport.AgentMessage
import mu.KotlinLogging

class JsonAgentMessageDeserializer {

    private val logger = KotlinLogging.logger {}
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun <T : AgentMessage> deserialize(
        deserializer: DeserializationStrategy<T>,
        message: ByteArray
    ) = ByteArrayInputStream(message).use {
        try {
            json.decodeFromStream(deserializer, it)
        } catch (e: SerializationException) {
            logger.error(e) { "deserialize: Deserialization error: $e" }
            null
        }
    }

}
