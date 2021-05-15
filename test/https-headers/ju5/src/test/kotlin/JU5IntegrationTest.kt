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
import com.epam.drill.test.common.*
import org.junit.jupiter.api.*
import java.util.*
import kotlin.reflect.jvm.*
import kotlin.test.*
import kotlin.test.Test

@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
class JU5IntegrationTest {

    @Test
    fun simpleTestMethodName() {
        expectedTests.add(::simpleTestMethodName.toTestData(engine, TestResult.PASSED))
        assertTrue(true)
    }

    @Test
    fun `method with backtick names`() {
        expectedTests.add(::`method with backtick names`.toTestData(engine, TestResult.PASSED))
        assertTrue(true)
    }

    @Test
    fun `Кириллик леттерс`() {
        expectedTests.add(::`Кириллик леттерс`.toTestData(engine, TestResult.PASSED))
        assertTrue(true)
    }

    @Test
    fun `shortBacktick`() {
        expectedTests.add(::`shortBacktick`.toTestData(engine, TestResult.PASSED))
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

        private const val engine = "junit-jupiter"
        private val sessionId = "${UUID.randomUUID()}"
        private val expectedTests = mutableListOf(
            JU5IntegrationTest::testSkipped.toTestData(engine, TestResult.SKIPPED),
        )

        @BeforeAll
        @JvmStatic
        fun startSession() {
            SessionProvider.startSession(sessionId)
        }

        @AfterAll
        @JvmStatic
        fun checkTests() {
            SessionProvider.stopSession(sessionId)
            val serverDate: ServerDate = getAdminData()
            val testFromAdmin = serverDate.tests[sessionId] ?: emptyList()
            testFromAdmin shouldContainsAllTests expectedTests
            testFromAdmin.assertTestTime()
        }
    }
}
