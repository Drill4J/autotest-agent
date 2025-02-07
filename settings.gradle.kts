rootProject.name = "autotest-agent"

pluginManagement {
    val kotlinVersion: String by extra
    val licenseVersion: String by extra
    val grgitVersion: String by extra
    val shadowPluginVersion: String by extra
    val psxpaulExecforkVersion: String by extra
    val nexusPublishPluginVersion: String by extra
    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.noarg") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.ajoberstar.grgit") version grgitVersion
        id("com.github.hierynomus.license") version licenseVersion
        id("com.github.johnrengelman.shadow") version shadowPluginVersion
        id("com.github.psxpaul.execfork") version psxpaulExecforkVersion
        id("io.github.gradle-nexus.publish-plugin") version nexusPublishPluginVersion
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

val sharedLibsLocalPath: String by extra
val includeSharedLib: Settings.(String) -> Unit = {
    include(it)
    project(":$it").projectDir = file(sharedLibsLocalPath).resolve(it)
}

includeSharedLib("logging")
includeSharedLib("common")
includeSharedLib("jvmapi")
includeSharedLib("agent-instrumentation")
includeSharedLib("agent-transport")
includeSharedLib("agent-config")
includeSharedLib("konform")
includeSharedLib("knasm")
includeSharedLib("kt2dts")
includeSharedLib("kt2dts-api-sample")
includeSharedLib("kt2dts-cli")
include("autotest-logging")
include("autotest-agent")
