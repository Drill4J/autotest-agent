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
import com.sun.net.httpserver.*
import java.net.*


class AdminStub(private val host: String, private val port: String) {

    init {
        if (host.isEmpty() || port.isEmpty()) {
            throw RuntimeException("Host or Port is not present. Application could not start")
        }
    }

    fun startServer() {
        val storage = AdminStubStorage()
        val httpServer = HttpServer.create(InetSocketAddress(host, port.toInt()), 0)
        httpServer.createContext("/status") { httpExchange ->
            val response = storage.dump().toByteArray()
            httpExchange.sendResponseHeaders(200, response.size.toLong())
            httpExchange.responseBody.use {
                it.write(response)
            }
        }
        httpServer.createContext("/api/agents/test-pet-standalone/plugins/test2code/dispatch-action") { httpExchange ->
            val string = httpExchange.requestBody.reader().readText()
            storage.addAction(string)
            val response = "OK".toByteArray()
            httpExchange.sendResponseHeaders(200, response.size.toLong())
            httpExchange.responseBody.use {
                it.write(response)
            }
        }
        httpServer.createContext("/api/login") { httpExchange ->
            if (httpExchange.requestMethod == "POST") {
                val response = "OK".toByteArray()
                httpExchange.responseHeaders.add("authorization", "token")
                httpExchange.sendResponseHeaders(200, response.size.toLong())
                httpExchange.responseBody.use {
                    it.write(response)
                }
            }
        }
        httpServer.start()
        println("Server has started ${httpServer.address}")
    }

}


