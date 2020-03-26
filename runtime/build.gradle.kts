import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("com.github.johnrengelman.shadow") version "5.1.0"
    kotlin("jvm")
    java
}

val javassistVersion = "3.18.1-GA"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("org.javassist:javassist:$javassistVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("drillRuntime.jar")
}