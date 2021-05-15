subprojects {
    dependencies {
        implementation(project(":test:commonTest"))
        //TODO move to commonTest module
        implementation(project(":")) { isTransitive = false }
    }
    configure<com.epam.drill.agent.runner.AgentConfiguration> {
        additionalParams = mutableMapOf(
            "sessionId" to "testSession",
            "browserProxyAddress" to "host.docker.internal:7777",
            "isRealtimeEnable" to "false",
            "isGlobal" to "false",
            "isManuallyControlled" to "true",
            "rawFrameworkPlugins" to "cucumber"
        )
    }
}
