package com.epam.drill.test.agent.config

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.native.concurrent.*

@SharedImmutable
val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

infix fun <T> KSerializer<T>.parse(rawData: String) = json.decodeFromString(this, rawData)

infix fun <T> KSerializer<T>.stringify(rawData: T) = json.encodeToString(this, rawData)
