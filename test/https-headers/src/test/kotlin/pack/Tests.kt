package pack

import com.google.gson.*
import com.mashape.unirest.http.*
import okhttp3.*
import org.apache.http.client.methods.*
import org.apache.http.impl.client.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.*
import java.net.*

const val TEST_NAME_HEADER = "drill-test-name"
const val SESSION_ID_HEADER = "drill-session-id"

class Tests {

    @Test
    fun simpleTestMethodName() {
        test(::simpleTestMethodName.name)
    }

    @Test
    fun `method with backtick names`() {
        test(::`method with backtick names`.name)
    }

    @Suppress("RemoveRedundantBackticks")
    @Test
    fun `shortBacktick`() {
        test(::`shortBacktick`.name)
    }

    private fun test(methodName: String) {
        sequenceOf(
            "http://postman-echo.com/headers",
            "https://postman-echo.com/headers"
        ).forEach {
            clients.forEach { client -> client(methodName, it) }
        }
    }

    private val clients: Sequence<(String, String) -> Unit> =
        sequenceOf(::externalApacheCall, ::externalJavaCall, ::externalUnirestCall, ::externalOkHttpCall)

    private fun externalApacheCall(methodName: String, url: String) {
        val response = HttpClients.createDefault().execute(HttpGet(url))
        check(methodName, bodyToMap(response.entity.content.reader()))
    }

    private fun externalUnirestCall(methodName: String, url: String) {
        val reader = Unirest.get(url).asBinary().body.reader()
        check(methodName, bodyToMap(reader))
    }

    private fun externalOkHttpCall(methodName: String, url: String) {
        val response = OkHttpClient().newCall(Request.Builder().url(url).build()).execute()
        check(methodName, bodyToMap(response.body()!!.byteStream().reader()))
    }

    private fun externalJavaCall(methodName: String, url: String) {
        val con: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        check(methodName, bodyToMap(con.inputStream.reader()))
    }

    private fun bodyToMap(inpt: InputStreamReader) =
        Gson().fromJson(inpt, Map::class.java)["headers"] as Map<*, *>

    private fun check(methodName: String, headersContainer: Map<*, *>) {
        assertEquals("${Tests::class.simpleName}:$methodName", headersContainer[TEST_NAME_HEADER])
        assertEquals("testSession", headersContainer[SESSION_ID_HEADER])
    }
}