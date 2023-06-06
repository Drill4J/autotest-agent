import java.net.URI
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import com.epam.drill.agent.runner.LogLevels

plugins {
    kotlin("jvm")
    id("com.github.hierynomus.license")
    id("com.epam.drill.agent.runner.autotest")
}

group = "com.epam.drill.autotest"
version = rootProject.version

val restAssuredVersion: String by parent!!.extra
val junitJupiterVersion: String by parent!!.extra
val nativeAgentLibName: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
}

tasks {
    test {
        useJUnitPlatform()
        dependsOn(":autotest-agent:install${HostManager.host.presetName.capitalize()}Dist")
        dependsOn(":tests-admin-stub-server:serverStart")
    }
}

val nativeAgentDir = project(":autotest-agent").buildDir.resolve("install").resolve(HostManager.host.presetName)
val nativeAgentFile = "${HostManager.host.family.dynamicPrefix}${nativeAgentLibName.replace("-", "_")}.${HostManager.host.family.dynamicSuffix}"
drill {
    runtimePath = nativeAgentDir
    agentPath = nativeAgentDir.resolve(nativeAgentFile)
    agentId = "test-pet-standalone"
    adminHost = rootProject.extra["testsAdminStubServerHost"] as String
    adminPort = rootProject.extra["testsAdminStubServerPort"] as Int
    logLevel = LogLevels.TRACE
    jvmArgs += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006"
    labels = mapOf("User" to "Test", "Team" to "Drill4j")
    additionalParams = mapOf(
        "sessionId" to "testSession",
        "browserProxyAddress" to "host.docker.internal:7777",
        "isGlobal" to "false",
        "isRealtimeEnable" to "false",
        "isManuallyControlled" to "true"
    )
}

@Suppress("UNUSED_VARIABLE")
license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
    val licenseFormatSources by tasks.registering(LicenseFormat::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/kni", "**/commonGenerated")
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/kni", "**/commonGenerated")
        }
    }
}
