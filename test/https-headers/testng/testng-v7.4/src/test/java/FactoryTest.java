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
import com.epam.drill.plugins.test2code.api.TestResult;
import com.epam.drill.test.agent.instrumentation.testing.testng.TestNGStrategy;
import com.epam.drill.test.common.TestData;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FactoryTest extends BaseTest {
    private Integer param;

    @Factory(dataProvider = "dataMethod")
    public FactoryTest(int param) {
        this.param = param;
    }

    @DataProvider
    public static Object[][] dataMethod() {
        return new Object[][]{{0}, {1}};
    }

    @Test
    public void testMethodOne() {
        String opValue = param + "1";
        System.out.println("Test method one output: " + opValue);
        Companion.getExpectedTests().add(toData("testMethodOne"));
    }

    @Test
    public void testMethodTwo() {
        String opValue = param + "2";
        System.out.println("Test method two output: " + opValue);
        Companion.getExpectedTests().add(toData("testMethodTwo"));
    }

    TestData toData(String method) {
        return new TestData(
                TestNGStrategy.engineSegment + "/[class:" + getClass().getSimpleName() + "(" + param.getClass().getSimpleName() + ")[" + param + "]" + "]/[method:" + method + "()" + "]",
                TestResult.PASSED
        );
    }
}
