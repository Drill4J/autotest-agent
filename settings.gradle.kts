rootProject.name = "autotest-agent"

pluginManagement {
    val kotlinVersion: String by extra
    val licenseVersion: String by extra
    val grgitVersion: String by extra
    val shadowPluginVersion: String by extra
    plugins {
        kotlin("multiplatform") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        id("org.ajoberstar.grgit") version grgitVersion
        id("com.github.hierynomus.license") version licenseVersion
        id("com.github.johnrengelman.shadow") version shadowPluginVersion
    }
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

val includeSharedLib: Settings.(String) -> Unit = {
    include(it)
    project(":$it").projectDir = file("lib-jvm-shared/$it")
}
