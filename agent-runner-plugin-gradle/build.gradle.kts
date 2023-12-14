import java.net.URI
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

@Suppress("RemoveRedundantBackticks")
plugins {
    `signing`
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.autotest"
version = rootProject.version

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

gradlePlugin {
    plugins {
        create("runner") {
            id = "$group.runner"
            implementationClass = "com.epam.drill.test.agent.runner.AutoTestAgent"
            displayName = "Runner-plugin for Gradle"
            description = "Autotest-agent runner-plugin for Gradle"
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("gradle-plugin"))
    implementation(project(":agent-runner-common"))
}

@Suppress("UNUSED_VARIABLE")
tasks {
    val compileKotlin by getting(KotlinCompile::class) {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
    }
}

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("Runner-plugin for Gradle")
            description.set("Autotest-agent runner-plugin for Gradle")
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
