/*
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


import com.epam.drill.SessionProvider
import com.epam.drill.plugins.test2code.api.TestDetails
import com.epam.drill.plugins.test2code.api.TestInfo
import com.epam.drill.plugins.test2code.api.TestResult
import com.epam.drill.test.agent.instrumentation.testing.junit.JUnitStrategy
import com.epam.drill.test.common.AssertKt
import com.epam.drill.test.common.ServerDate
import com.epam.drill.test.common.TestData
import com.epam.drill.test.common.UtilKt
import org.junit.experimental.categories.Category
import spock.lang.Shared
import spock.lang.Specification

@Category(UnitTest.class)
class MessageServiceSpec extends Specification {

    @Shared
    private Set<TestData> actualTests = new HashSet<>();

    def 'Get message'() {
        def testHash = getTestHash(specificationContext.currentIteration.name)
        actualTests.add new TestData(testHash, TestResult.PASSED)
        URLConnection con = (URLConnection) new URL("http://postman-echo.com/headers").openConnection()
        def bytes = con.inputStream.bytes
        expect: 'Should return the correct message'
        println 'Should return the correct message'
        new String(bytes).contains(testHash)
    }

    def "numbers to the power of two"(int a, int b, int c) {
        def testHash = getTestHash(specificationContext.currentIteration.name)
        actualTests.add new TestData(testHash, TestResult.PASSED)
        URLConnection con = (URLConnection) new URL("http://postman-echo.com/headers").openConnection()
        def bytes = con.inputStream.bytes
        expect:
        new String(bytes).contains(testHash)
        where:
        a | b | c
        1 | 2 | 1
        2 | 2 | 4
        3 | 2 | 9
    }


    @Shared
    private String sessionId = UUID.randomUUID().toString()

    def setupSpec() {
        SessionProvider.INSTANCE.startSession(sessionId, "AUTO", false, "", false)
    }

    def cleanupSpec() {
        SessionProvider.INSTANCE.stopSession(sessionId)
        ServerDate serverDate = UtilKt.getAdminData()
        List<TestInfo> testFromAdmin = serverDate.getTests().get(sessionId)
        AssertKt.shouldContainsAllTests(testFromAdmin, actualTests)
        AssertKt.assertTestTime(testFromAdmin)
    }

    String getTestHash(String name) {
        def params = new HashMap<String, String>()
        params.put("methodParams", "()")
        params.put("classParams", "")
        return com.epam.drill.test.agent.util.UtilKt.hash(new TestDetails(JUnitStrategy.engineSegment, MessageServiceSpec.class.getName(), name, params, new HashMap<>()))
    }
}
