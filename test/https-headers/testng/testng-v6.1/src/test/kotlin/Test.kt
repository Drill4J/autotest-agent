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
import org.testng.annotations.*
import pack.*


class TestTestNG {

    @Test
    fun simpleTestMethodName() {
        HttpHeadersTest.test(::simpleTestMethodName.name)
    }

    @Test
    fun `method with backtick names`() {
        HttpHeadersTest.test(::`method with backtick names`.name)
    }

    companion object {

        private const val sessionId = "testSession"

        @BeforeClass
        @JvmStatic
        fun startSession() {
            SessionProvider.startSession(sessionId)
        }

        @AfterClass
        @JvmStatic
        fun stopSession() {
            SessionProvider.stopSession(sessionId)
        }
    }
}
