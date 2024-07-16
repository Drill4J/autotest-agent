import java.net.ServerSocket
import java.net.URI
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import com.github.psxpaul.task.JavaExecFork

plugins {
    kotlin("jvm")
    id("com.github.hierynomus.license")
    id("com.github.psxpaul.execfork")
}

group = "com.epam.drill.autotest"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":common"))
    implementation(project(":test2code-api"))
    implementation(project(":tests-common"))
}

@Suppress("UNUSED_VARIABLE")
tasks {
    val (host, port) = ServerSocket(0).use { "127.0.0.1" to it.localPort }
    rootProject.extra["testsAdminStubServerHost"] = host
    rootProject.extra["testsAdminStubServerPort"] = port
    val serverStart by creating(JavaExecFork::class) {
        group = "verification"
        workingDir = jar.get().archiveFile.get().asFile.parentFile
        classpath = sourceSets.main.get().runtimeClasspath
        main = "MainKt"
        jvmArgs = mutableListOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5015")
        args = mutableListOf(host, port.toString())
        waitForPort = port
        killDescendants = false
    }
    serverStart.dependsOn(jar)
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
