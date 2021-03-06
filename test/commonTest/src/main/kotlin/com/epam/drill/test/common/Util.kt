/**
 * Copyright 2020 EPAM Systems
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
package com.epam.drill.test.common

import com.epam.drill.plugins.test2code.api.*
import kotlinx.serialization.json.*
import org.apache.http.client.methods.*
import org.apache.http.impl.client.*
import java.lang.reflect.*
import java.net.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*

fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

val json = Json {
    encodeDefaults = true
}

fun parseAction(
    rawAction: String,
): Action = json.decodeFromString(Action.serializer(), rawAction)

fun ServerDate.encode() = json.encodeToString(ServerDate.serializer(), this)

fun String.decodeServerDate() = json.decodeFromString(ServerDate.serializer(), this)

fun getAdminData() = run {
    val host = System.getenv("host")
    val port = System.getenv("port")
    HttpClients.createDefault().execute(
        HttpGet("http://$host:$port/status")
    ).entity.content.reader().readText().decodeServerDate()
}

fun Method.toTestData(
    engine: String,
    testResult: TestResult,
    supportParam: Boolean = false,
): TestData = TestData(name = "$engine/[class:${declaringClass.name}]/[method:$name(" +
        "${if (supportParam) parameters.joinToString(",") else ""})]",
    testResult = testResult)

fun KFunction<*>.toTestData(
    engine: String,
    testResult: TestResult,
    supportParam: Boolean = false,
): TestData = javaMethod!!.toTestData(engine, testResult, supportParam)

fun String.cucumberTestToTestData(
    engine: String,
    featurePath: String,
    testResult: TestResult,
) = TestData("$engine/[feature:$featurePath]/[scenario:$this]", testResult)

fun TestInfo.toTestData() = TestData(name, result)
