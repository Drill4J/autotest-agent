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
import com.epam.drill.test.agent.instrumentation.testing.junit.*
import com.epam.drill.test.common.*
import com.epam.drill.test.common.TestData
import org.junit.*
import org.junit.Ignore
import org.junit.Test
import java.util.*
import kotlin.test.*

@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
class JU4IntegrationTest {

    @Test
    fun simpleTestMethodName() {
        expectedTests.add(::simpleTestMethodName.toTestData(JUnitStrategy.engineSegment, TestResult.PASSED))
        assertTrue(true)
    }

    @Test
    fun `method with backtick names`() {
        expectedTests.add(::`method with backtick names`.toTestData(JUnitStrategy.engineSegment, TestResult.PASSED))
        assertTrue(true)
    }

    @Test
    fun `Кириллик леттерс`() {
        expectedTests.add(::`Кириллик леттерс`.toTestData(JUnitStrategy.engineSegment, TestResult.PASSED))
        assertTrue(true)
    }

    @Ignore
    @Test
    fun testSkipped() {
        assertTrue(false)
    }

    // TODO Figure out how to test the case when the test fails
//    @Test
//    fun testFailed() {
//        assertTrue(false)
//    }

    companion object {

        private val sessionId = "${UUID.randomUUID()}"
        private val expectedTests = mutableListOf(
            JU4IntegrationTest::testSkipped.toTestData(JUnitStrategy.engineSegment, TestResult.SKIPPED)
        )

        @BeforeClass
        @JvmStatic
        fun startSession() {
            SessionProvider.startSession(sessionId)
        }

        @AfterClass
        @JvmStatic
        fun checkTests() {
            SessionProvider.stopSession(sessionId)
            val serverDate: ServerDate = getAdminData()
            val testsFromAdmin = serverDate.tests[sessionId] ?: emptyList()
            testsFromAdmin shouldContainsAllTests expectedTests
            testsFromAdmin.assertTestTime()
        }
    }
}
