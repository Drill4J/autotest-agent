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
package com.epam.drill.agent.test.prioritization

import com.epam.drill.agent.test.configuration.Configuration
import com.epam.drill.agent.test.configuration.ParameterDefinitions
import com.epam.drill.agent.test.testinfo.CLASS_PARAMS_KEY
import com.epam.drill.agent.test.testinfo.METHOD_PARAMS_KEY
import com.epam.drill.agent.test2code.api.TestDetails
import mu.KotlinLogging

object RecommendedTests {
    private val logger = KotlinLogging.logger {}
    private val recommendedTestsReceiver: RecommendedTestsReceiver = RecommendedTestsReceiverImpl()
    private val recommendedTestsEnabled: Boolean
        get() = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_ENABLED]
    private val testsToSkip: Set<TestDetails> by lazy { initTestsToSkip() }

    private fun initTestsToSkip(): Set<TestDetails> {
        if (!recommendedTestsEnabled) return emptySet()
        val groupId = Configuration.parameters[ParameterDefinitions.GROUP_ID]
        val testTaskId = Configuration.parameters[ParameterDefinitions.TEST_TASK_ID]
        val filterCoverageDays = Configuration.parameters[ParameterDefinitions.RECOMMENDED_TESTS_FILTER_COVERAGE_DAYS].toInt()
        return recommendedTestsReceiver.getTestsToSkip(groupId, testTaskId, filterCoverageDays.takeIf { it > 0 }).toSet()
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