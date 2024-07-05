package com.epam.drill.test.agent.configuration

import com.benasher44.uuid.uuid4
import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.DefaultParameterDefinitions

class RuntimeParametersProvider(
    override val priority: Int = 100
) : AgentConfigurationProvider {

    override val configuration = configuration()

    private fun configuration() = mapOf(
        Pair(DefaultParameterDefinitions.INSTANCE_ID.name, uuid4().toString())
    )

}
