package pack

import com.sun.net.httpserver.*
import org.apache.http.client.methods.*
import org.apache.http.impl.client.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.*

const val TEST_NAME_HEADER = "drill-test-name"

class Tests {

    companion object {

        var httpServer: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
        var port: Int


        init {
            httpServer.createContext("/echo") { t ->
                val response = "OK"
                t.responseHeaders.add(
                    TEST_NAME_HEADER, t.requestHeaders.getFirst(
                        TEST_NAME_HEADER
                    ) ?: "empty"
                )
                t.sendResponseHeaders(200, response.toByteArray().size.toLong())
                val os = t.responseBody
                os.write(response.toByteArray())
                os.close()
            }
            httpServer.executor = null
            port = httpServer.address.port
        }

        @BeforeAll
        @JvmStatic
        fun startSimpleEchoServer() {
            httpServer.start()
        }

        @AfterAll
        @JvmStatic
        fun destroySimpleEchoServer() {
            httpServer.stop(1)
        }
    }

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
        val client = HttpClients.createDefault()!!
        val request = HttpPost("http://localhost:$port/echo")
        val response = client.execute(request)
        assertEquals(methodName, response.getHeaders(TEST_NAME_HEADER)[0].value)
    }
}