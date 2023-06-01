import java.net.URI
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("jvm")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.autotest"
version = rootProject.version

val cucumberVersion = "6.5.1"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("io.cucumber:cucumber-java:$cucumberVersion")
    compileOnly("io.cucumber:cucumber-junit:$cucumberVersion")

    implementation("org.seleniumhq.selenium:selenium-java:4.0.0-alpha-4")
    implementation("io.github.bonigarcia:webdrivermanager:3.8.1")
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
