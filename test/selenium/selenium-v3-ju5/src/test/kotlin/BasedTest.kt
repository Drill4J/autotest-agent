package abs

import com.sun.net.httpserver.*
import org.junit.jupiter.api.*
import org.openqa.selenium.*
import java.net.*

abstract class BasedTest {

    internal lateinit var driver: WebDriver
    private var httpServer: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
    var port: Int
    val testNames = mutableMapOf<String, Int>()


    init {
        httpServer.createContext("/1") { t ->
            val response = "OK"
            val ss = t.requestHeaders["drill-test-name"]?.firstOrNull() ?: ""
            val i = testNames[ss]
            if (i == null) {
                testNames[ss] = 0
            } else testNames[ss] = i + 1
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


    companion object{

        @BeforeAll
        @JvmStatic
        fun bf(){

        }

        @AfterAll
        fun af(){

        }

    }

    abstract fun setupDriver()


    @Test
    fun itCanBeAnyTestName1() {
        var localCount = -1
        repeat(1000) {
            driver.get("http://localhost:$port/1")
            localCount += 2
            Assertions.assertEquals(testNames.filterKeys { it.contains(::itCanBeAnyTestName1.name)}.values.first(), localCount)

        }
        driver.get("http://localhost:$port/1")
    }

    @Test
    fun itCanBeAnyTestName2() {
        var localCount = -1
        repeat(400) {
            driver.get("http://localhost:$port/1")
            localCount += 2
            Assertions.assertEquals(testNames.filterKeys { it.contains(::itCanBeAnyTestName2.name)}.values.first(), localCount)

        }
        driver.get("http://localhost:$port/1")
    }

}