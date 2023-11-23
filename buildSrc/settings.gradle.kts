pluginManagement {
    val kotlinVersion: String by extra
    val licenseVersion: String by extra
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        id("com.github.hierynomus.license") version licenseVersion
    }
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

include("agent-runner-common")
include("agent-runner-gradle")
project(":agent-runner-common").projectDir = rootDir.parentFile.resolve("agent-runner-common")
project(":agent-runner-gradle").projectDir = rootDir.parentFile.resolve("agent-runner-gradle")
