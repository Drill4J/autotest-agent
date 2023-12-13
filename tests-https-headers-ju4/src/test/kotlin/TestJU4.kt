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
import com.epam.drill.test.agent.instrument.strategy.testing.junit.*
import com.epam.drill.test.agent.session.*
import com.epam.drill.test.common.*
import org.junit.*
import pack.*
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore
@Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")
class Test {

    @Test
    @Ignore
    fun simpleTestMethodName() {
        HttpHeadersTest.test(::simpleTestMethodName.toTestData(JUnitStrategy.engineSegment, TestResult.SKIPPED).hash)
    }

    @Test
    fun `method with backtick names`() {
        HttpHeadersTest.test(::`method with backtick names`.toTestData(JUnitStrategy.engineSegment, TestResult.PASSED).hash)
    }

    @Test
    fun `Кириллик леттерс`() {
        HttpHeadersTest.test(::`Кириллик леттерс`.toTestData(JUnitStrategy.engineSegment, TestResult.PASSED).hash)
    }

    companion object {

        private const val sessionId = "testSession"

        @BeforeClass
        @JvmStatic
        fun startSession() {
//            SessionProvider.startSession(sessionId)
        }

        @AfterClass
        @JvmStatic
        fun stopSession() {
//            SessionProvider.stopSession(sessionId)
        }
    }
}
