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
package com.epam.drill.test.agent.instrument.servers

import com.epam.drill.agent.instrument.servers.ReactorTransformerObject

import com.epam.drill.agent.instrument.AbstractTransformerObject
import com.epam.drill.agent.instrument.ClassPathProvider
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.test.agent.instrument.RuntimeClassPathProvider
import com.epam.drill.test.agent.instrument.reactor.transformers.FluxTransformer
import com.epam.drill.test.agent.instrument.reactor.transformers.MonoTransformer
import com.epam.drill.test.agent.instrument.reactor.transformers.ParallelFluxTransformer
import com.epam.drill.test.agent.instrument.reactor.transformers.SchedulersTransformer

private val reactorTransformers = setOf<AbstractTransformerObject>(
    FluxTransformer,
    MonoTransformer,
    ParallelFluxTransformer,
    SchedulersTransformer
)

actual object ReactorTransformer :
    TransformerObject,
    ReactorTransformerObject(reactorTransformers),
    ClassPathProvider by RuntimeClassPathProvider