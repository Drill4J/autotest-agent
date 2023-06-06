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
import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.agent.instrumentation.testing.testng.*
import com.epam.drill.test.common.*
import org.testng.annotations.*
import pack.*

class TestTestNG : BaseTest() {

    @Test
    fun simpleTestMethodName() {
        val toTestData = ::simpleTestMethodName.toTestData(TestNGStrategy.engineSegment, TestResult.PASSED)
        HttpHeadersTest.test(toTestData.hash)
        expectedTests.add(toTestData)
    }

    @Test
    fun `method with backtick names`() {
        val toTestData = ::`method with backtick names`.toTestData(TestNGStrategy.engineSegment, TestResult.PASSED)
        HttpHeadersTest.test(toTestData.hash)
        expectedTests.add(toTestData)
    }
}
