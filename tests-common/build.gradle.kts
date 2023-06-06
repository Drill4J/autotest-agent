import java.net.URI
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.autotest"
version = rootProject.version

val kotlinxSerializationVersion: String by parent!!.extra
val squareupOkHttpVersion: String by parent!!.extra
val googleGsonVersion: String by parent!!.extra
val restAssuredVersion: String by parent!!.extra
val unirestJavaVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(kotlin("test"))
    implementation("com.google.code.gson:gson:$googleGsonVersion")
    implementation("com.mashape.unirest:unirest-java:$unirestJavaVersion")
    implementation("io.rest-assured:rest-assured:$restAssuredVersion")
    implementation(project(":autotest-agent")) { isTransitive = false }

    api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    api(project(":test2code-api"))
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
