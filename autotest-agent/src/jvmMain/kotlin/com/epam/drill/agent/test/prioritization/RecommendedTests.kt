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

import com.epam.drill.agent.test2code.api.TestDetails
import mu.KotlinLogging

object RecommendedTests {
    private val logger = KotlinLogging.logger {}
    private val recommendedTestsReceiver: RecommendedTestsReceiver = RecommendedTestsReceiverImpl()
    private val testsToSkip: Set<TestDetails> by lazy { initTestsToSkip() }

    private fun initTestsToSkip() = recommendedTestsReceiver.getTestsToSkip()
        .toSet()
        .also {
            logger.info { "${it.size} tests will be skipped by Drill4J" }
        }


    fun shouldSkip(
        engine: String,
        testClass: String,
        testMethod: String,
        methodParameters: String? = null
    ): Boolean {
        val test = TestDetails(
            engine = engine,
            path = testClass,
            testName = testMethod,
            testParams = methodParameters?.split(",")?.toList() ?: emptyList(),
        )
        return shouldSkipByTestDetails(test)
    }

    fun shouldSkipByTestDetails(test: TestDetails): Boolean {
        return testsToSkip.contains(test).also {
            if (it) {
                logger.debug { "Test `${test.testName}` will be skipped by Drill4J" }
                recommendedTestsReceiver.sendSkippedTest(test)
            } else {
                logger.debug { "Test `${test.testName}` will not be skipped by Drill4J" }
                if ("testFormatByMask" in test.testName) {
                    logger.debug { "!!! Test to skip `${test}`" }
                    testsToSkip.filter { it.testName == test.testName }.forEach {
                        logger.debug { "!!! Test candidate `${it}` " }
                    }
                }
            }
        }
    }

}