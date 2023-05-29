package com.epam.drill.test.agent.js

import kotlinx.serialization.*

@Serializable
data class JsSessionData(
    val coverage: String,
    val scripts: String,
    val testId: String,
)
