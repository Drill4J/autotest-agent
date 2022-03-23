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
package com.epam.drill.test.agent.util

import com.epam.drill.logger.Logging
import com.epam.drill.plugins.test2code.api.*
import java.net.*
import java.util.zip.*
import java.lang.Long.*

val perfLogger = Logging.logger("PerfLogger")

fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

fun TestDetails.hash(): String = toString().crc32()

fun String.crc32(): String = CRC32().let {
    it.update(toByteArray())
    toHexString(it.value)
}
