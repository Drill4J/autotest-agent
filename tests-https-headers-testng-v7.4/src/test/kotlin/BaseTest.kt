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
import com.epam.drill.test.agent.session.*
import com.epam.drill.test.common.*
import org.slf4j.*
import org.testng.annotations.*

abstract class BaseTest {
    protected val sessionId = "testSession"

    @JvmField
    protected var logger: Logger = LoggerFactory.getLogger("Testng-7.4-logger")

    companion object {
        val expectedTests = mutableSetOf<TestData>()
    }

    @BeforeSuite
    fun beforeSuite() {
        getAdminData()
        SessionProvider.startSession(sessionId)
    }

    @AfterSuite
    open fun checkTests() {
        SessionProvider.stopSession(sessionId)
        val serverDate: ServerDate = getAdminData()
        val testFromAdmin = serverDate.tests[sessionId] ?: emptyList()
        testFromAdmin shouldContainsAllTests expectedTests
        testFromAdmin.assertTestTime()
    }

}
