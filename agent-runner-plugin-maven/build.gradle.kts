import java.net.URI
import org.jetbrains.kotlin.konan.target.HostManager
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

@Suppress("RemoveRedundantBackticks")
plugins {
    `signing`
    `maven-publish`
    kotlin("jvm")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.autotest"
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
        from(project(":agent-runner-common").sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
    val javadocJar by registering(Jar::class) {
        dependsOn(project(":agent-runner-common").tasks.javadoc)
        from(javadoc.get().destinationDir)
        from(project(":agent-runner-common").tasks.javadoc.get().destinationDir)
        archiveClassifier.set("javadoc")
    }
    val mvnInstall by registering(Exec::class) {
        val args = if (HostManager.hostIsMingw) arrayOf("cmd", "/c", "mvnw.cmd") else arrayOf("sh", "./mvnw")
        commandLine(*args, "install", "-Ddrill.plugin.version=$version", "-Dkotlin.version=$kotlinVersion")
        workingDir(project.projectDir)
        standardOutput = System.out
        outputs.file("target/agent-runner-plugin-maven-$version.jar")
    }
    assemble.get().dependsOn(sourcesJar)
    assemble.get().dependsOn(javadocJar)
    assemble.get().dependsOn(mvnInstall)
    clean {
        delete("target")
    }
}

publishing {
    publications.create<MavenPublication>("agent-runner-plugin-maven") {
        artifact(tasks["sourcesJar"])
        artifact(tasks["javadocJar"])
        artifact(tasks["mvnInstall"].outputs.files.singleFile).builtBy(tasks["mvnInstall"])
        pom {
            name.set("Runner-plugin for Maven")
            description.set("Autotest-agent runner-plugin for Maven")
            withXml {
                asNode().appendNode("dependencies").appendNode("dependency").apply {
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
