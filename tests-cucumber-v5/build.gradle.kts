import java.net.URI
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("jvm")
    id("com.github.hierynomus.license")
    id("com.epam.drill.autotest.runner")
}

group = "com.epam.drill.autotest"
version = rootProject.version

val cucumberVersion = "5.4.0"
val nativeAgentLibName: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.seleniumhq.selenium:selenium-java:4.0.0-alpha-4")
    implementation("io.github.bonigarcia:webdrivermanager:3.8.1")
    implementation(project(":http-clients-instrumentation"))
    implementation(project(":tests-common"))

    api(project(":autotest-agent")) { isTransitive = false }

    testImplementation("io.cucumber:cucumber-testng:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
    testImplementation(project(":tests-cucumber-common"))
}

tasks {
    test {
        useJUnit()
        dependsOn(":autotest-agent:install${HostManager.host.presetName.capitalize()}Dist")
        dependsOn(":tests-admin-stub-server:serverStart")
    }
    clean {
        delete("target")
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
    logLevel = "TRACE"
    jvmArgs += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5011"
    labels = mapOf("User" to "Test", "Team" to "Drill4j")
    additionalParams = mapOf(
        "sessionId" to "testSession",
        "browserProxyAddress" to "host.docker.internal:7777",
        "isGlobal" to "false",
        "isRealtimeEnable" to "false",
        "isManuallyControlled" to "true",
        "rawFrameworkPlugins" to "cucumber"
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
