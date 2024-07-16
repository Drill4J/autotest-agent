import java.net.URI
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("jvm")
    id("com.github.hierynomus.license")
}

group = "com.epam.drill.autotest"
version = rootProject.version

val squareupOkHttpVersion: String by parent!!.extra
val unirestJavaVersion: String by parent!!.extra
val googleGsonVersion: String by parent!!.extra

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("test"))
    implementation("com.google.code.gson:gson:$googleGsonVersion")
    implementation("com.mashape.unirest:unirest-java:$unirestJavaVersion")
    implementation("com.squareup.okhttp3:okhttp:$squareupOkHttpVersion")
    implementation(project(":tests-common"))

    api(project(":autotest-agent")) { isTransitive = false }
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
