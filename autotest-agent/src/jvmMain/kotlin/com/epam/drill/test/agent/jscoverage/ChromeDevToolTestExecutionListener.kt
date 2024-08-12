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
package com.epam.drill.test.agent.jscoverage

import com.epam.drill.plugins.test2code.api.TestResult
import com.epam.drill.test.agent.instrument.strategy.selenium.DevToolStorage
import com.epam.drill.test.agent.instrument.strategy.selenium.WebDriverThreadStorage
import com.epam.drill.test.agent.testinfo.TestExecutionListener
import com.epam.drill.test.agent.testinfo.TestLaunchInfo

class ChromeDevToolTestExecutionListener(
    private val jsCoverageSender: JsCoverageSender
): TestExecutionListener {

    override fun onTestStarted(test: TestLaunchInfo) {
        DevToolStorage.get()?.startIntercept()
        WebDriverThreadStorage.addCookies()
    }

    override fun onTestFinished(test: TestLaunchInfo, result: TestResult) {
        DevToolStorage.get()?.stopIntercept()
        jsCoverageSender.sendJsCoverage(test.testLaunchId)
    }

    override fun onTestIgnored(test: TestLaunchInfo) {
        DevToolStorage.get()?.stopIntercept()
    }
}