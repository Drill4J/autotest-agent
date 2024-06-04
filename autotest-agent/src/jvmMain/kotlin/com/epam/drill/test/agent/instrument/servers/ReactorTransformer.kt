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