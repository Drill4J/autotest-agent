import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName

plugins {
    groovy
    kotlin("jvm")
    id("com.epam.drill.autotest.runner")
}

group = "com.epam.drill.autotest"
version = rootProject.version

val javassistVersion: String by parent!!.extra
val nativeAgentLibName: String by parent!!.extra

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.javassist:javassist:$javassistVersion")
    implementation(project(":agent-instrumentation"))
    implementation(project(":logging"))
    implementation(project(":common"))
    implementation(project(":knasm"))
    implementation(project(":tests-common"))

    api(project(":autotest-agent")) { isTransitive = false }

    testImplementation("org.codehaus.groovy:groovy-all:2.4.11")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4")
}

tasks {
    test {
        useJUnit {
            includeCategories = setOf("UnitTest")
        }
        testLogging.showStandardStreams = true
        environment("host" to rootProject.extra["testsAdminStubServerHost"])
        environment("port" to rootProject.extra["testsAdminStubServerPort"])
        dependsOn(":autotest-agent:install${HostManager.host.presetName.capitalize()}Dist")
        dependsOn(":tests-admin-stub-server:serverStart")
    }
}

val nativeAgentDir = project(":autotest-agent").buildDir.resolve("install").resolve(HostManager.host.presetName)
val nativeAgentFile = "${HostManager.host.family.dynamicPrefix}${nativeAgentLibName.replace("-", "_")}.${HostManager.host.family.dynamicSuffix}"
drill {
    runtimePath = nativeAgentDir
    agentPath = nativeAgentDir.resolve(nativeAgentFile)
    appId = "test-pet-standalone"
    drillApiUrl = "http://" + rootProject.extra["testsAdminStubServerHost"] as String + ":" + rootProject.extra["testsAdminStubServerPort"] as Int
    logLevel = "TRACE"
    jvmArgs += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5007"
    labels = mapOf("User" to "Test", "Team" to "Drill4j")
    additionalParams = mapOf(
        "sessionId" to "testSession",
        "browserProxyAddress" to "host.docker.internal:7777",
        "isGlobal" to "false",
        "isRealtimeEnable" to "false",
        "isManuallyControlled" to "true"
    )
}
