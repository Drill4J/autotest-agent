import java.net.URI
import org.jetbrains.kotlin.konan.target.HostManager
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    `maven-publish`
    kotlin("jvm")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.agent.runner"
version = rootProject.version

val kotlinVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.apache.maven:maven-core:3.8.1")
    implementation("org.apache.maven:maven-plugin-api:3.8.1")
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.1")
    implementation("org.apache.maven.plugins:maven-surefire-plugin:2.22.2")
    implementation("org.twdata.maven:mojo-executor:2.3.2")
    implementation(project(":agent-runner-common"))
}

@Suppress("UNUSED_VARIABLE")
tasks {
    val sourcesJar by registering(Jar::class) {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
    val install by registering(Exec::class) {
        val args = if (HostManager.hostIsMingw) arrayOf("cmd", "/c", "mvnw.cmd") else arrayOf("sh", "./mvnw")
        commandLine(*args, "install", "-Ddrill.plugin.version=$version", "-Dkotlin.version=$kotlinVersion")
        workingDir(project.projectDir)
        standardOutput = System.out
    }
    publish.get().dependsOn(install)
    publishToMavenLocal.get().dependsOn(install)
}


publishing {
    publications {
        create<MavenPublication>("drill-maven") {
            artifact(tasks["sourcesJar"])
            artifact(file("target/maven-$version.jar"))
            pom.withXml {
                this.asNode().appendNode("dependencies").appendNode("dependency").apply {
                    appendNode("groupId", "org.jetbrains.kotlin")
                    appendNode("artifactId", "kotlin-stdlib")
                    appendNode("version", kotlinVersion)
                }
            }
        }
    }
}

@Suppress("UNUSED_VARIABLE")
license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
    val licenseFormatSources by tasks.registering(LicenseFormat::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
        }
    }
}
