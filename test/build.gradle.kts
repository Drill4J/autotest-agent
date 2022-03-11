import com.epam.drill.agent.runner.LogLevels.*
import org.jetbrains.kotlin.konan.target.*
import java.net.*

plugins {
    kotlin("jvm")
    id("com.epam.drill.agent.runner.autotest") version "0.3.1" apply false
}
val jupiterVersion: String by rootProject
val gsonVersion: String by rootProject
val restAssuredVersion: String by rootProject
val unirestVersion: String by rootProject
val okHttpVersion: String by rootProject
val slf4jVersion: String by rootProject

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
val presetName = HostManager.host.presetName
val (host, port) = ServerSocket(0).use { "127.0.0.1" to it.localPort }

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.epam.drill.agent.runner.autotest")
    dependencies {
        implementation("io.rest-assured:rest-assured:$restAssuredVersion")
        implementation("com.google.code.gson:gson:$gsonVersion")
        implementation("com.mashape.unirest:unirest-java:$unirestVersion")
        implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
        implementation("org.slf4j:slf4j-simple:$slf4jVersion")
        implementation(kotlin("reflect"))
        implementation(project(":runtime"))
    }


    if (this.name.endsWith("-ju5")) {
        println("Engine jupiter: ${this.name}")
        dependencies {
            testImplementation("org.junit.jupiter:junit-jupiter:$jupiterVersion")
        }
        this.tasks.withType<Test> {
            processServer {
                useJUnitPlatform()
            }
        }
    }

    tasks.withType<Test> {
        processServer {
            dependsOn(project(":").tasks.getByPath("linkAutoTestAgentDebugShared${presetName.capitalize()}"))
            dependsOn(project(":").tasks.getByPath("install${presetName.capitalize()}Dist"))
        }
    }

    project.extra["adminHost"] = host
    project.extra["adminPort"] = port

    configure<com.epam.drill.agent.runner.AgentConfiguration> {
        additionalParams = mutableMapOf(
            "sessionId" to "testSession",
            "browserProxyAddress" to "host.docker.internal:7777",
            "isRealtimeEnable" to "false",
            "isGlobal" to "false",
            "isManuallyControlled" to "true"
        )
        runtimePath = rootProject.file("./build/install/$presetName")
        agentPath = rootProject
            .file("./build/install/$presetName")
            .resolve("${HostManager.host.family.dynamicPrefix}autoTestAgent.${HostManager.host.family.dynamicSuffix}")//todo
        agentId = "test-pet-standalone"
        adminHost = host
        adminPort = port
        logLevel = TRACE
        jvmArgs = jvmArgs + "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006"
        labels = mapOf("User" to "Test", "Team" to "Drill4j")
    }


}

fun Test.processServer(block: Task.() -> Unit) {
    environment("host" to host)
    environment("port" to port)
    dependsOn(":test:admin-stub-server:startServer")
    block()
    finalizedBy(":test:admin-stub-server:stopServer")
}
