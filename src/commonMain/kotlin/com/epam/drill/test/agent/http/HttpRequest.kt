package com.epam.drill.test.agent.http

import kotlinx.serialization.Serializable

@Serializable
data class HttpRequest(val method: String, val headers: Map<String, String> = mapOf(), val body: String="")