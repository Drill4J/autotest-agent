package com.epam.drill.auto.test.agent.config

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.native.concurrent.*

@SharedImmutable
val json = Json(JsonConfiguration.Stable)

infix fun <T> KSerializer<T>.parse(rawData: String) = json.parse(this, rawData)

infix fun <T> KSerializer<T>.stringify(rawData: T) = json.stringify(this, rawData)
