subprojects {
    dependencies {
        implementation(project(":test:commonTest"))
    }
    configure<com.epam.drill.agent.runner.AgentConfiguration> {
        additionalParams = mutableMapOf(
            "rawFrameworkPlugins" to "cucumber"
        )
    }
}
