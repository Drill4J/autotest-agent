import java.net.URI
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    implementation("io.konform:konform:0.4.0")
}

@Suppress("UNUSED_VARIABLE")
tasks {
    val compileKotlin by getting(KotlinCompile::class) {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
    }
}

publishing {
    publications.create<MavenPublication>("agent-runner-common") {
        from(components["java"])
        pom {
            name.set("Runner-plugin common library")
            description.set("Autotest-agent runner-plugin common library")
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
