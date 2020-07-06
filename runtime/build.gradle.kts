@file:Suppress("ObjectLiteralToLambda")

import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("com.github.johnrengelman.shadow") version "5.1.0"
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    java
    distribution
}

val javassistVersion = "3.27.0-GA"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}
val serializationRuntimeVersion: String by rootProject
val drillLoggerVersion: String by rootProject
val websocketVersion: String by rootProject

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("org.javassist:javassist:$javassistVersion")
    implementation("org.java-websocket:Java-WebSocket:$websocketVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
    implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

distributions {
    create("extension") {
        distributionBaseName.set("extension")
        contents {
            from(file("drill-header-transmitter"))
            eachFile (object : Action<FileCopyDetails> {
            override fun execute(fcp: FileCopyDetails) {
                fcp.relativePath =  RelativePath(true, fcp.relativePath.pathString.replace("extension/", ""))
            }
        })
        }
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    val extensionDistZip = tasks.getByPath("extensionDistZip")
    dependsOn(extensionDistZip)
    doFirst {
        extensionDistZip.outputs.files.singleFile
            .renameTo(
                buildDir
                    .resolve("resources")
                    .resolve("main").apply { mkdirs() }
                    .resolve("header-transmitter.xpi")
            )
    }
    archiveFileName.set("drillRuntime.jar")
    relocate("kotlin", "kruntime")
    relocate("javassist", "drill.javassist")
    relocate("org.java_websocket", "drill.org.java_websocket")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.UnstableDefault"
}
