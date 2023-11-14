import java.net.URI
import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.agent.runner"
version = rootProject.version

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

gradlePlugin {
    plugins {
        create("autotest") {
            id = "$group.autotest"
            implementationClass = "com.epam.drill.autotest.gradle.AutoTestAgent"
        }
        create("app") {
            id = "$group.app"
            implementationClass = "com.epam.drill.autotest.gradle.AppAgent"
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
