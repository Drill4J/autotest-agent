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
package com.epam.drill.test.common

import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.agent.TestListener.classParamsKey
import com.epam.drill.test.agent.TestListener.methodParamsKey
import com.google.gson.*
import kotlinx.serialization.json.*
import org.apache.http.client.methods.*
import org.apache.http.impl.client.*
import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*


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
    testClass: KClass<*>?,
    testResult: TestResult,
    parameters: List<Any?>,
    paramNumber: String,
): TestData = run {
    val testFullName = TestDetails(
        engine = engine,
        path = testClass?.jvmName ?: declaringClass.name,
        testName = name,
        params = mapOf(
            classParamsKey to "",
            methodParamsKey to (parameters.takeIf { it.any() }?.let {
                parameters.joinToString(",", "(", ")") {
                    it?.javaClass?.simpleName?.substringBeforeLast("\$") ?: it.toString()
                } + "[$paramNumber]"
            } ?: "()")),
        metadata = emptyMap(),
        labels = emptySet(),
    ).also { println("Expected: $it") }.hash()
    TestData(testFullName, testResult)
}

fun KFunction<*>.toTestData(
    engine: String,
    testResult: TestResult,
    testClass: KClass<*>? = null,
    parameters: List<Any?> = emptyList(),
    paramNumber: String = "",
): TestData = javaMethod!!.toTestData(engine, testClass, testResult, parameters, paramNumber)

fun String.cucumberTestToTestData(
    engine: String,
    featurePath: String,
    testResult: TestResult,
) = TestData(
    TestDetails(
        engine = engine,
        path = featurePath,
        testName = this,
        params = mapOf(
            classParamsKey to "",
            methodParamsKey to "()"
        ),
        metadata = emptyMap(),
        labels = emptySet(),
    ).hash(),
    testResult
)

fun TestInfo.toTestData() = TestData(id, result)

