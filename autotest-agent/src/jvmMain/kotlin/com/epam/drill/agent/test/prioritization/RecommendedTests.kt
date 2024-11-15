package com.epam.drill.agent.test.prioritization

import com.epam.drill.agent.test.testinfo.CLASS_PARAMS_KEY
import com.epam.drill.agent.test.testinfo.METHOD_PARAMS_KEY
import com.epam.drill.agent.test2code.api.TestDetails
import mu.KotlinLogging

object RecommendedTests {
    private val logger = KotlinLogging.logger {}
    private val testsToSkip = mutableSetOf<TestDetails>()

    init {
        //TODO stub data
        testsToSkip.apply {
            add(
                TestDetails(
                    engine = "junit-jupiter",
                    path = "com.epam.drill.compatibility.testframeworks.JUnit5Test",
                    testName = "simpleTestMethodName",
                )
            )
            add(
                TestDetails(
                    engine = "testng",
                    path = "com.epam.drill.compatibility.testframeworks.TestNG7Test",
                    testName = "simpleTestMethodName",
                    params = mapOf(METHOD_PARAMS_KEY to "()"),
                )
            )
            add(
                TestDetails(
                    engine = "testng",
                    path = "com.epam.drill.compatibility.testframeworks.TestNG6Test",
                    testName = "simpleTestMethodName",
                    params = mapOf(METHOD_PARAMS_KEY to "()"),
                )
            )
            add(
                TestDetails(
                    engine = "junit",
                    path = "com.epam.drill.compatibility.testframeworks.JUnit4Test",
                    testName = "simpleTestMethod",
                )
            )
            add(
                TestDetails(
                    engine = "cucumber",
                    path = "classpath:features/example.feature",
                    testName = "Add two numbers",
                )
            )
        }
    }

    fun shouldSkip(
        engine: String,
        testClass: String,
        testMethod: String,
        methodParameters: String? = null,
        classParameters: String? = null
    ): Boolean {
        val params = mutableMapOf<String, String>()
        methodParameters?.let { params[METHOD_PARAMS_KEY] = it }
        classParameters?.let { params[CLASS_PARAMS_KEY] = it }
        val test = TestDetails(
            engine = engine,
            path = testClass,
            testName = testMethod,
            params = params,
        )
        return shouldSkipByTestDetails(test)
    }

    fun shouldSkipByTestDetails(test: TestDetails): Boolean {
        logger.trace { "Test path = ${test.path} name = ${test.testName} is being checked by Drill4J..." }
        return testsToSkip.contains(test).also {
            if (it) {
                logger.info { "Test path = ${test.path} name = ${test.testName} is skipped by Drill4J" }
            } else {
                logger.debug { "Test path = ${test.path} name = ${test.testName} is recommended by Drill4J" }
            }
        }
    }

}