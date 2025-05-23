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
package com.epam.drill.agent.test.configuration

import kotlin.reflect.KProperty
import mu.KotlinLogging
import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.DefaultParameterDefinitions
import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.test.configuration.ParameterDefinitions
import com.epam.drill.agent.konform.validation.Invalid
import com.epam.drill.agent.konform.validation.Validation
import com.epam.drill.agent.konform.validation.ValidationError
import com.epam.drill.agent.konform.validation.ValidationErrors
import com.epam.drill.agent.konform.validation.ValidationResult
import com.epam.drill.agent.konform.validation.jsonschema.minLength
import com.epam.drill.agent.konform.validation.jsonschema.minimum

class ValidatedParametersProvider(
    private val configurationProviders: Set<AgentConfigurationProvider>,
    override val priority: Int = Int.MAX_VALUE
) : AgentConfigurationProvider {

    private class ValidatingParameters(provider: ValidatedParametersProvider) {
        val appId by provider
        val groupId by provider
        val drillInstallationDir by provider
        val apiUrl by provider
        val apiKey by provider
        val devToolsProxyAddress by provider
        val browserProxyAddress by provider
        val logLevel by provider
        val logLevelAsList = logLevel?.split(";") ?: emptyList()
        val logLimit by provider
        val logLimitAsInt = logLimit?.toIntOrNull()
    }

    private val strictValidators = Validation<ValidatingParameters> {
        ValidatingParameters::drillInstallationDir required {
            minLength(1)
        }
        ValidatingParameters::apiUrl required {
            validTransportUrl()
        }
    }

    private val softValidators = Validation<ValidatingParameters> {
        ValidatingParameters::appId ifPresent {
            identifier()
            minLength(3)
        }
        ValidatingParameters::groupId ifPresent {
            identifier()
            minLength(3)
        }
        ValidatingParameters::apiKey ifPresent {
            minLength(3)
        }
        ValidatingParameters::logLevelAsList onEach {
            isValidLogLevel()
        }
        ValidatingParameters::logLimitAsInt ifPresent {
            minimum(0)
        }
        ValidatingParameters::devToolsProxyAddress ifPresent {
            validTransportUrl()
        }
        ValidatingParameters::browserProxyAddress ifPresent {
            validHostPortSpec()
        }
    }

    private val logger = KotlinLogging.logger("com.epam.drill.agent.test.configuration.ValidatedParametersProvider")

    private val validatingConfiguration = validatingConfiguration()

    override val configuration = validateConfiguration()

    private fun validatingConfiguration() = configurationProviders
        .sortedBy(AgentConfigurationProvider::priority)
        .map(AgentConfigurationProvider::configuration)
        .reduce { acc, map -> acc + map }

    private fun validateConfiguration() = mutableMapOf<String, String>().also { defaultValues ->
        val defaultFor: (AgentParameterDefinition<out Any>) -> Unit = {
            defaultValues[it.name] = it.defaultValue.toString()
        }
        val isInvalid: (ValidationResult<*>) -> Boolean = { it is Invalid }
        strictValidators(ValidatingParameters(this)).takeIf(isInvalid)?.let { result ->
            val message = "Cannot load the agent because some agent parameters are set incorrectly. " +
                    convertToMessage(result.errors)
            logger.error { message }
            throw ParameterValidationException(message)
        }
        softValidators(ValidatingParameters(this)).takeIf(isInvalid)?.let { result ->
            val message = "Some agent parameters were set incorrectly and were replaced with default values. " +
                    convertToMessage(result.errors)
            logger.error { message }
            result.errors.forEach { error ->
                when (convertToField(error)) {
                    ValidatingParameters::appId.name -> defaultFor(DefaultParameterDefinitions.APP_ID)
                    ValidatingParameters::groupId.name -> defaultFor(DefaultParameterDefinitions.GROUP_ID)
                    ValidatingParameters::logLevel.name -> defaultFor(ParameterDefinitions.LOG_LEVEL)
                    ValidatingParameters::logLimit.name -> defaultFor(ParameterDefinitions.LOG_LIMIT)
                }
            }
        }
    }

    private fun convertToMessage(errors: ValidationErrors) = "Please check the following parameters:\n" +
            errors.joinToString("\n") { " - ${convertToField(it)} ${it.message}" }

    private fun convertToField(error: ValidationError) = error.dataPath.removePrefix(".")
        .substringBeforeLast("AsList")
        .removeSuffix("AsInt")

    private operator fun getValue(thisRef: Any, property: KProperty<*>) =
        validatingConfiguration[property.name]

}
