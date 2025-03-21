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
package com.epam.drill.agent.test.instrument.strategy.testing.testng

import javassist.*
import java.security.*

@Suppress("unused")
object TestNGStrategyV7 : TestNGStrategy() {

    override val versionRegex: Regex = "testng-7\\.[0-9]+(\\.[0-9]+)*".toRegex()
    override val id: String = "testng7"

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        return if ("${ctClass.url}".contains(versionRegex)) {
            super.instrument(ctClass, pool, classLoader, protectionDomain)
        } else {
            null
        }
    }
}
