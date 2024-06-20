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
package com.epam.drill.test.agent.runner

import io.konform.validation.Validation
import io.konform.validation.jsonschema.minLength

val validateConfig = Validation<Configuration> {
    Configuration::groupId required {
        identifier()
        minLength(3)
    }
    Configuration::appId required {
        identifier()
        minLength(3)
    }
    Configuration::drillApiUrl required {
        validTransportUrl()
    }
    Configuration::drillApiKey ifPresent {
        minLength(1)
    }
    Configuration::logLevel ifPresent {
        isValidLogLevel()
    }
}

fun validateConfiguration(config: Configuration) {
    val validationResult = validateConfig.validate(config)
    if (validationResult.errors.isNotEmpty()) {
        val message = "Drill4J Agent Runner parameters are set incorrectly. " +
                "Please check the following parameters:\n" +
                validationResult.errors.joinToString("\n") { field -> " - ${field.dataPath} ${field.message}" }
        throw IllegalArgumentException(message)
    }
}