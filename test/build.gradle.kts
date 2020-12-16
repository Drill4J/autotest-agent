import com.epam.drill.agent.runner.LogLevels.*
import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("jvm")
    id("com.epam.drill.agent.runner.autotest") apply false
}
val jupiterVersion = "5.6.2"
val gsonVersion = "2.8.5"
val restAssuredVersion = "4.0.0"
allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
    }
}
val presetName = HostManager.host.presetName
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.epam.drill.agent.runner.autotest")
    dependencies {
        implementation("io.rest-assured:rest-assured:$restAssuredVersion")
        implementation("com.google.code.gson:gson:$gsonVersion")
        implementation("com.mashape.unirest:unirest-java:1.4.9")
        implementation("com.squareup.okhttp3:okhttp:3.12.0")
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("reflect"))
        implementation(project(":runtime"))
    }


    if (this.name.endsWith("-ju5")) {
        println("Engine jupiter: ${this.name}")
        dependencies {
            testImplementation("org.junit.jupiter:junit-jupiter:$jupiterVersion")
        }
        this.tasks.withType<Test> {
            useJUnitPlatform()
        }
    }
    this.tasks.withType<Test> {
        dependsOn(project(":").tasks.getByPath("linkAutoTestAgentDebugShared${presetName.capitalize()}"))
        dependsOn(project(":").tasks.getByPath("install${presetName.capitalize()}Dist"))
    }

    configure<com.epam.drill.agent.runner.AgentConfiguration> {
        additionalParams = mutableMapOf(
            "sessionId" to "testSession",
            "browserProxyAddress" to "host.docker.internal:7777",
            "isRealtimeEnable" to "false",
            "isGlobal" to "false"
        )
        runtimePath = rootProject.file("./build/install/$presetName")
        agentPath = rootProject
            .file("./build/install/$presetName")
            .resolve("${HostManager.host.family.dynamicPrefix}autoTestAgent.${HostManager.host.family.dynamicSuffix}")//todo
        agentId = "test-pet-standalone"
        adminHost = "stub"
        adminPort = -1
        logLevel = TRACE
    }


}
