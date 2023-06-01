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
import com.epam.drill.*
import com.epam.drill.plugins.test2code.api.*
import com.epam.drill.test.agent.instrumentation.testing.junit.*
import com.epam.drill.test.common.*
import org.junit.jupiter.api.*
import pack.*
import kotlin.test.*
import kotlin.test.Test

class TestJU5 {
    private val engine = "junit-jupiter"

    @Test
    fun simpleTestMethodName() {
        HttpHeadersTest.test(::simpleTestMethodName.toTestData(engine, TestResult.PASSED).hash)
    }

    @Test
    fun `method with backtick names`() {
        HttpHeadersTest.test(::`method with backtick names`.toTestData(engine, TestResult.PASSED).hash)
    }

    companion object {

        private const val sessionId = "testSession"

        @BeforeAll
        @JvmStatic
        fun startSession() {
            SessionProvider.startSession(sessionId)
        }

        @AfterAll
        @JvmStatic
        fun stopSession() {
            SessionProvider.stopSession(sessionId)
        }
    }
}
