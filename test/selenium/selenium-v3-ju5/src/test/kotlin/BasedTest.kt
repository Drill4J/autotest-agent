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
package abs

import com.sun.net.httpserver.*
import org.junit.jupiter.api.*
import org.openqa.selenium.*
import java.net.*

abstract class BasedTest {

    internal lateinit var driver: WebDriver
    private var httpServer: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
    private var port: Int
    private val testNames = mutableMapOf<String, Int>()


    init {
        httpServer.createContext("/1") { exchange ->
            val response = "OK"
            val testNameHeader = exchange.requestHeaders["drill-test-name"]?.firstOrNull() ?: ""
            val count = testNames[testNameHeader]
            if (count == null) {
                testNames[testNameHeader] = 0
            } else testNames[testNameHeader] = count + 1
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()
        }
        httpServer.executor = null
        port = httpServer.address.port
    }

    @BeforeEach
    fun beforeAll() {
        httpServer.start()
        setupDriver()
    }


    @AfterEach
    fun after() {
        driver.quit()
        httpServer.stop(0)
    }


    companion object {
        @BeforeAll
        @JvmStatic
        fun bf() {
        }

        @AfterAll
        fun af() {
        }

    }

    abstract fun setupDriver()

    @Test
    fun itCanBeAnyTestName1() {
        checkHeaderInjected(::itCanBeAnyTestName1.name)
    }

    @Test
    fun itCanBeAnyTestName2() {
        checkHeaderInjected(::itCanBeAnyTestName2.name, repeatCount = 10)
    }

    private fun checkHeaderInjected(testName: String, repeatCount: Int = 20) {
        var localCount = -1
        repeat(repeatCount) {
            driver.get("http://localhost:$port/1")
            localCount += 1
            Assertions.assertEquals(
                testNames.filterKeys { it.contains(testName) }.values.first(),
                localCount
            )
        }
        driver.get("http://localhost:$port/1")
    }

}
