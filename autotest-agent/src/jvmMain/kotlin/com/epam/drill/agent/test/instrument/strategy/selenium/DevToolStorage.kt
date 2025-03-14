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
package com.epam.drill.agent.test.instrument.strategy.selenium

import mu.KotlinLogging

object DevToolStorage {
    private val logger = KotlinLogging.logger {}
    private val storage: InheritableThreadLocal<ChromeDevTool> = InheritableThreadLocal()

    fun set(devtool: ChromeDevTool) {
        storage.set(devtool)
        logger.debug { "DevTool inited for: Thread id=${Thread.currentThread().id}, DevToolWS address=$devtool" }
    }

    fun get(): ChromeDevTool? = storage.get()

    fun clear() = storage.remove()
}
