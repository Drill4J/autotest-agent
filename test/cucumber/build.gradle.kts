
allprojects {
    dependencies {
        implementation("io.github.bonigarcia:webdrivermanager:3.8.1")
        implementation("org.seleniumhq.selenium:selenium-java:4.0.0-alpha-4")
    }
}

subprojects {
    dependencies {
        implementation(project(":test:commonTest"))
        //TODO move to commonTest module
        api(project(":")) { isTransitive = false }
        testImplementation(project(":test:cucumber"))
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

val cucumberVersion = "6.5.1"
dependencies {
    compileOnly("io.cucumber:cucumber-java:$cucumberVersion")
    compileOnly("io.cucumber:cucumber-junit:$cucumberVersion")
}

