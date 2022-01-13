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
import com.epam.drill.*
import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.agent.instrumentation.testing.testng.*
import com.epam.drill.test.common.*
import org.testng.*
import org.testng.Assert.*
import org.testng.annotations.*
import pack.*

class RetriedTest : BaseTest() {

    private val sleepTime = 600L

    @Test(invocationCount = 2)
    fun testWithRetryListener() {
        val toTestData = ::testWithRetryListener.toTestData(TestNGStrategy.engineSegment, TestResult.PASSED)
        expectedTests.add(toTestData)
        Thread.sleep(sleepTime)
    }


    @AfterSuite
    override fun checkTests() {
        SessionProvider.stopSession(sessionId)
        val serverDate: ServerDate = getAdminData()
        val testFromAdmin = serverDate.tests[sessionId] ?: emptyList()
        testFromAdmin shouldContainsAllTests expectedTests
        testFromAdmin.assertTestTime()
        val testInfo = testFromAdmin.first()
        assertTrue((testInfo.finishedAt - testInfo.startedAt) >= sleepTime * 2)
    }
}
