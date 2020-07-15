package abs

import com.sun.net.httpserver.*
import io.github.bonigarcia.wdm.*
import org.junit.jupiter.api.*
import org.openqa.selenium.*
import org.openqa.selenium.chrome.*
import java.net.*


class Junit5Selenium4Test {

    companion object {
        private var httpServer: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
        var port: Int
        val testNames = mutableSetOf<String>()

        init {
            httpServer.createContext("/1") { t ->
                val response = "OK"
                testNames.add(t.requestHeaders["drill-test-name"]?.firstOrNull() ?: "")
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
        Assertions.assertTrue(testNames.contains(this::class.simpleName + ":" + ::itCanBeAnyTestName.name))
    }

    @Test
    fun itCanBeAnyTestName2() {
        driver.get("http://localhost:$port/1")
        Assertions.assertTrue(testNames.contains(this::class.simpleName + ":" + ::itCanBeAnyTestName2.name))
    }
}