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
package pack

import io.restassured.RestAssured.*
import io.restassured.builder.*
import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.*


const val TEST_ID_HEADER = "drill-test-id"
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
                    .expectBody(containsString(TEST_ID_HEADER))
                    .expectBody(containsString(SESSION_ID_HEADER))
                    .build()
            )
    }


}
