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
package com.epam.drill.agent.test.instrument

import com.epam.drill.agent.test.instrument.StrategyManager
import mu.KotlinLogging

actual object AgentClassTransformer {

    private val logger = KotlinLogging.logger {}

    const val CLASS_NAME = "AgentClassTransformer"

    actual fun transform(
        className: String,
        classBytes: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? = when (className) {
        "io/netty/util/internal/logging/Log4J2Logger" -> null
        else -> runCatching {
            StrategyManager.process(className, classBytes, loader, protectionDomain)
        }.onFailure {
            com.epam.drill.agent.test.instrument.AgentClassTransformer.logger.warn(it) { "Can't instrument '${className}' class." }
        }.getOrNull()
    }

}
