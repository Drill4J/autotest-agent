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
package seleniumhq

import com.codeborne.selenide.*
import com.sun.net.httpserver.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*
import org.testcontainers.containers.*
import org.testcontainers.containers.wait.strategy.*
import org.testcontainers.junit.jupiter.*
import java.net.*
import kotlin.reflect.*


private const val PROXY_PORT = 7777

const val dispNameForTest1 = "tstName"
const val testName = "TestName"

@DisplayName(testName)
@Disabled
@Testcontainers
internal class SelenideSimpleUITest {

    @TestFactory
    fun dynamicTestsWithCollection(): Collection<DynamicTest?>? {
        val firsTestName = "first test"
        val secondTestName = "second Test"
        return listOf(DynamicTest.dynamicTest(firsTestName) {
            Selenide.open("http://host.docker.internal:$port/1")
            println(testHashes)
            Assertions.assertTrue(testHashes.any { it.contains(::dynamicTestsWithCollection.name) })
        },
            DynamicTest.dynamicTest(secondTestName) {
                Selenide.open("http://host.docker.internal:$port/1")
                Assertions.assertTrue(testHashes.any { it.contains(::dynamicTestsWithCollection.name) })
            }
        )
    }


    @BeforeEach
    fun before() {
        Selenide.open("http://host.docker.internal:$port/1")
    }

    @AfterEach
    fun after() {
        testHashes.clear()
    }

    @DisplayName(dispNameForTest1)
    @Test
    fun test1() {
        Assertions.assertTrue(testHashes.any { it.contains(::test1.name) })
    }

    @Test
    fun test2() {
        Assertions.assertTrue(testHashes.any { it.contains(::test2.name) })
    }

    @ParameterizedTest
    @CsvSource(
        "1,1",
        "2,2",
        "3,3",
        "4,4"
    )
    fun parameterizedTest(first: String, second: String?) {
        val fn = ::parameterizedTest
        val signature = fn.parameters.map { (it.type.classifier as KClass<*>).simpleName }
            .joinToString(separator = ", ", prefix = "(", postfix = ")")
        Assertions.assertTrue(testHashes.any { it.contains(fn.name) })
    }

    companion object {
        @org.testcontainers.junit.jupiter.Container
        val seleniumHub: KGenericContainer = KGenericContainer("selenium/standalone-chrome:3.141.59-20200525")
            .apply {
                withExposedPorts(4444)
                waitingFor(Wait.defaultWaitStrategy())
            }

        @Suppress("unused")
        @org.testcontainers.junit.jupiter.Container
        val browserProxy: KFixedContainer = KFixedContainer("drill4j/browser-proxy:0.1.0")
            .apply {
                withFixedExposedPort(PROXY_PORT, PROXY_PORT)
                waitingFor(Wait.forHttp("/").forStatusCode(400).forPort(PROXY_PORT)) //fixme
            }


        private var httpServer: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
        var port: Int
        val testHashes = mutableSetOf<String>()

        init {
            httpServer.createContext("/1") { t ->
                val response = "OK"
                testHashes.add(t.requestHeaders["drill-test-id"]?.firstOrNull() ?: "")
                t.sendResponseHeaders(200, response.toByteArray().size.toLong())
                val os = t.responseBody
                os.write(response.toByteArray())
                os.close()
            }
            httpServer.executor = null
            port = httpServer.address.port
        }


        @Suppress("unused")
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            httpServer.start()
            Configuration.browser = "chrome"
            Configuration.remote = "http://localhost:${seleniumHub.firstMappedPort}/wd/hub"
            Configuration.browserCapabilities.setCapability("enableVNC", true)
        }

        @Suppress("unused")
        @AfterAll
        @JvmStatic
        fun afterAll() {
            httpServer.stop(0)
        }
    }
}

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
class KFixedContainer(imageName: String) : FixedHostPortGenericContainer<KFixedContainer>(imageName)
