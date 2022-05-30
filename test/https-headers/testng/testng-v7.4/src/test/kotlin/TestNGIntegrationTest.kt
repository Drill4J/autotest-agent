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

import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.agent.instrumentation.testing.testng.*
import com.epam.drill.test.common.*
import org.testng.Assert.*
import org.testng.annotations.*

@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
class TestNGIntegrationTest : BaseTest() {

    @DataProvider
    fun dataProvider() = arrayOf<Array<Any?>>(
        arrayOf(1, "first", { str: String -> println("First: $str") }),
        arrayOf(2, "second", { str: String -> println("First: $str") }),
        arrayOf(null, null, null)
    )

    @Test
    fun simpleTestMethodName() {
        expectedTests.add(::simpleTestMethodName.toTestData(TestNGStrategy.engineSegment, TestResult.PASSED))
        assertTrue(true)
    }

    @Test
    fun `method with backtick names`() {
        expectedTests.add(::`method with backtick names`.toTestData(TestNGStrategy.engineSegment, TestResult.PASSED))
        assertTrue(true)
    }

    @Test
    fun `Кириллик леттерс`() {
        expectedTests.add(::`Кириллик леттерс`.toTestData(TestNGStrategy.engineSegment, TestResult.PASSED))
        assertTrue(true)
    }

    @Test(dataProvider = "dataProvider")
    fun parametrizedTest(int: Integer?, string: String?, lambda: ((String) -> Unit)?) {
        val paramNumber = dataProvider().indexOfFirst {
            it.contentEquals(arrayOf(int, string, lambda))
        }
        expectedTests.add(
            ::parametrizedTest.toTestData(
                TestNGStrategy.engineSegment,
                TestResult.PASSED,
                parameters = listOf(int, string, lambda),
                paramNumber = "$paramNumber"
            )
        )
        assertTrue(true)
    }

//    TODO EPMDJ-8321 Due to the fact that we may get skipped tests before or after the start of the suite, the test is temporarily disabled
//    @Ignore
//    @Test()
//    fun testSkipped() {
//        assertTrue(false)
//    }

    // TODO Figure out how to test the case when the test fails
//    @Test
//    fun testFailed() {
//        assertTrue(false)
//    }
}
