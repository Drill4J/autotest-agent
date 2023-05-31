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
package abs

import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.common.*
import com.sun.net.httpserver.*
import io.github.bonigarcia.wdm.*
import org.junit.jupiter.api.*
import org.openqa.selenium.*
import org.openqa.selenium.chrome.*
import java.net.*


class Junit5Selenium4Test {

    companion object {
        private const val engine = "junit-jupiter"
        private var httpServer: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
        var port: Int
        val testNames = mutableSetOf<String>()

        init {
            httpServer.createContext("/1") { t ->
                val response = "OK"
                testNames.add(t.requestHeaders["drill-test-id"]?.firstOrNull() ?: "")
                t.sendResponseHeaders(200, response.toByteArray().size.toLong())
                val os = t.responseBody
                os.write(response.toByteArray())
                os.close()
            }
            httpServer.executor = null
            port = httpServer.address.port
        }


        @JvmStatic
        lateinit var driver: WebDriver

        @Suppress("unused")
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            httpServer.start()
            WebDriverManager.chromedriver().setup()
            val options = ChromeOptions()
            options.setHeadless(true)
            driver = ChromeDriver(options)
        }


        @AfterAll
        @JvmStatic
        fun after() {
            driver.quit()
            httpServer.stop(0)
        }
    }

    @Test
    fun itCanBeAnyTestName() {
        driver.get("http://localhost:$port/1")
        Assertions.assertTrue(testNames.any {
            it.contains(::itCanBeAnyTestName.toTestData(engine,
                TestResult.PASSED).hash)
        })
    }

    @Test
    fun itCanBeAnyTestName2() {
        driver.get("http://localhost:$port/1")
        Assertions.assertTrue(testNames.any {
            it.contains(::itCanBeAnyTestName2.toTestData(engine, TestResult.PASSED).hash)
        })
    }
}
