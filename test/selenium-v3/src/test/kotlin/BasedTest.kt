package abs

import com.sun.net.httpserver.*
import org.junit.jupiter.api.*
import org.openqa.selenium.*
import java.net.*


abstract class BasedTest {

    internal lateinit var driver: WebDriver
    private var httpServer: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
    var port: Int
    val testNames = mutableSetOf<String>()

    init {
        httpServer.createContext("/1") { t ->
            val response = "OK"
            testNames.add(t.requestHeaders.get("drill-test-name")?.firstOrNull()?:"")
            t.sendResponseHeaders(200, response.toByteArray().size.toLong())
            val os = t.responseBody
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

    abstract fun setupDriver()


    @Test
    fun itCanBeAnyTestName() {
        driver.get("http://localhost:$port/1")
        Assertions.assertTrue(testNames.contains(this::class.simpleName+":"+::itCanBeAnyTestName.name))
    }

    @Test
    fun itCanBeAnyTestName2() {
        driver.get("http://localhost:$port/1")
        Assertions.assertTrue(testNames.contains(this::class.simpleName+":"+::itCanBeAnyTestName2.name))
    }

}