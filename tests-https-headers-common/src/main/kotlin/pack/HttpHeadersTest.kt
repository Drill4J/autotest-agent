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
package pack

import com.google.gson.*
import com.mashape.unirest.http.*
import okhttp3.*
import org.apache.http.client.methods.*
import org.apache.http.impl.client.*
import java.io.*
import java.net.*
import kotlin.test.*

const val TEST_ID_HEADER = "drill-test-id"
const val SESSION_ID_HEADER = "drill-session-id"

object HttpHeadersTest {

    fun test(testHash: String) {
        sequenceOf(
            "http://postman-echo.com/headers",
            "https://postman-echo.com/headers"
        ).forEach {
            clients.forEach { client -> client(testHash, it) }
        }
    }

    private val clients: Sequence<(String, String) -> Unit> =
        sequenceOf(::externalApacheCall, ::externalJavaCall, ::externalUnirestCall, ::externalOkHttpCall)

    private fun externalApacheCall(testHash: String, url: String) {
        val response = HttpClients.createDefault().execute(HttpGet(url))
        check(testHash, bodyToMap(response.entity.content.reader()))
    }

    private fun externalUnirestCall(testHash: String, url: String) {
        val reader = Unirest.get(url).asBinary().body.reader()
        check(testHash, bodyToMap(reader))
    }

    private fun externalOkHttpCall(testHash: String, url: String) {
        val response = OkHttpClient().newCall(Request.Builder().url(url).build()).execute()
        check(testHash, bodyToMap(response.body()!!.byteStream().reader()))
    }

    private fun externalJavaCall(testHash: String, url: String) {
        val con: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        check(testHash, bodyToMap(con.inputStream.reader()))
    }

    private fun bodyToMap(inpt: InputStreamReader) =
        Gson().fromJson(inpt, Map::class.java)["headers"] as Map<*, *>

    private fun check(testHash: String, headersContainer: Map<*, *>) {
        assertTrue(headersContainer[TEST_ID_HEADER]?.toString()?.contains(testHash) ?: false)
        assertEquals("testSession", headersContainer[SESSION_ID_HEADER])
    }
}
