import java.net.URI
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

plugins {
    kotlin("jvm")
    id("com.github.hierynomus.license")
    id("com.github.johnrengelman.shadow")
}

group = "com.epam.drill.autotest"
version = rootProject.version

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":kni-runtime"))
}

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    shadowJar {
        archiveFileName.set("drillRuntime.jar")
        from(jar)
        relocate("kotlin", "kruntime")
        relocate("javassist", "drill.javassist")
        relocate("org.slf4j", "drill.org.slf4j")
        relocate("org.java_websocket", "drill.org.java_websocket")

    }
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
