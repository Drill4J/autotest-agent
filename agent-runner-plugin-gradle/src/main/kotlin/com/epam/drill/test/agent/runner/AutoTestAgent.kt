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
@file:Suppress("unused")

package com.epam.drill.test.agent.runner

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.process.JavaForkOptions
import kotlin.reflect.KClass

open class AutoTestAgent : Agent(){
    override val extensionClass: KClass<out Configuration> = AgentConfiguration::class
    override val taskType: Set<KClass<out JavaForkOptions>> = setOf(Test::class, JavaExec::class)

}
