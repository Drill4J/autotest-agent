package pack

import io.restassured.RestAssured.*
import io.restassured.builder.*
import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.*


const val TEST_NAME_HEADER = "drill-test-name"
const val SESSION_ID_HEADER = "drill-session-id"

class Tests {

    @Test
    fun httpTest() {
        echoHeaders("http://postman-echo.com/headers")
    }


    @Test
    fun httpsTest() {
        echoHeaders("https://postman-echo.com/headers")
    }

    private fun echoHeaders(url: String) {
        given().get(url).then()
            .spec(
                ResponseSpecBuilder()
                    .expectStatusCode(200)
                    .expectBody(containsString(TEST_NAME_HEADER))
                    .expectBody(containsString(SESSION_ID_HEADER))
                    .build()
            )
    }


}