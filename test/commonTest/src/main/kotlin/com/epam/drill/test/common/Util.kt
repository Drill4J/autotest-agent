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
import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.TestListener.methodParamsKey
import com.google.gson.*
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

fun ServerDate.encode() = Gson().toJson(this)

//TODO Exception: $$serializer cannot be cast to kotlinx.serialization.KSerializer due to relocate("kotlin", "kruntime")
fun String.decodeServerDate(): ServerDate = Gson().fromJson(this, ServerDate::class.java)

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
    paramNumber: String,
): TestData = run {
    val testFullName = TestDetails(
        engine = engine,
        path = declaringClass.name,
        testName = name,
        params = mapOf(methodParamsKey to (paramNumber.takeIf { it.isNotBlank() }?.let {
            parameters.joinToString(",", "(", ")") { it.type.simpleName } + "[$paramNumber]"
        } ?: "()")),
        metadata = emptyMap()
    ).fullName()
    TestData(testFullName, testResult)
}

fun KFunction<*>.toTestData(
    engine: String,
    testResult: TestResult,
    paramNumber: String = "",
): TestData = javaMethod!!.toTestData(engine, testResult, paramNumber)

fun String.cucumberTestToTestData(
    engine: String,
    featurePath: String,
    testResult: TestResult,
) = TestData(TestDetails(
    engine = engine,
    path = featurePath,
    testName = this,
    params = mapOf(methodParamsKey to "()"),
    metadata = emptyMap()).fullName(),
    testResult)

fun TestInfo.toTestData() = TestData(name, result)

