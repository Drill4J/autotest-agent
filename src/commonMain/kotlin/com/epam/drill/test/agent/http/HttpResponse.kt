package com.epam.drill.test.agent.http

import kotlinx.serialization.Serializable

@Serializable
data class HttpResponse(val code: Int, val headers: Map<String, String> = mapOf(), val body: String = "")