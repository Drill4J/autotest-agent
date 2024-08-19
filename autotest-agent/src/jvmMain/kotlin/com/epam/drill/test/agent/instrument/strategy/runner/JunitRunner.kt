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
package com.epam.drill.test.agent.instrument.strategy.runner

import com.epam.drill.agent.instrument.*
import com.epam.drill.test.agent.instrument.RuntimeClassPathProvider
import javassist.*
import java.util.*
import mu.KotlinLogging

class JunitRunner : AbstractTransformerObject(), ClassPathProvider by RuntimeClassPathProvider {

    override val logger = KotlinLogging.logger {}

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "org/junit/runner/JUnitCore"
    }

    override fun transform(className: String, ctClass: CtClass) {
        val sessionId = UUID.randomUUID()
        val method = ctClass.getMethod("run", "(Lorg/junit/runner/Runner;)Lorg/junit/runner/Result;")
        method.insertBefore("com.epam.drill.test.agent.Drill.startSession(\"$sessionId\");")
        method.insertAfter("com.epam.drill.test.agent.Drill.stopSession(\"$sessionId\");")
    }
}