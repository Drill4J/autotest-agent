/**
 * Copyright 2020 EPAM Systems
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.epam.drill.plugins.test2code.api.*;
import com.epam.drill.test.agent.*;
import com.epam.drill.test.agent.instrumentation.testing.testng.*;
import com.epam.drill.test.agent.util.*;
import com.epam.drill.test.common.TestData;
import org.testng.annotations.*;

import java.util.*;
import java.util.function.Consumer;

public class FactoryTest extends BaseTest {
    private Integer param;
    private String value;
    private Consumer<String> func;

    @Factory(dataProvider = "dataMethod")
    public FactoryTest(int param, String value, Consumer<String> func) {
        this.param = param;
        this.value = value;
        this.func = func;
    }

    @DataProvider
    public static Object[][] dataMethod() {
        Consumer<String> ff = str -> str.contains("FIRST");
        return new Object[][]{{0, "first", ff}, {1, "second", ff}};
    }

    @Test
    public void testMethodOne() {
        String opValue = param + "1";
        logger.info("Test method one output: " + opValue);
        Companion.getExpectedTests().add(toData("testMethodOne"));
    }

    @Test
    public void testMethodTwo() {
        String opValue = param + "2";
        logger.info("Test method two output: " + opValue);
        Companion.getExpectedTests().add(toData("testMethodTwo"));
    }

    TestData toData(String method) {
        HashMap<String, String> params = new HashMap<>();
        params.put(TestListener.classParamsKey, "(" + param.getClass().getSimpleName() + "," + value.getClass().getSimpleName() + ",Consumer)[" + param + "]");
        params.put("methodParams", "()");
        return new TestData(UtilKt.hash(new TestDetails(
                TestNGStrategy.engineSegment,
                getClass().getSimpleName(),
                method,
                params
        )), TestResult.PASSED);
    }
}
