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
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

val sharedLibsLocalPath: String by extra
val includeSharedLib: Settings.(String) -> Unit = {
    include(it)
    project(":$it").projectDir = file(sharedLibsLocalPath).resolve(it)
}

includeSharedLib("common")
includeSharedLib("jvmapi")
includeSharedLib("agent-instrumentation")
includeSharedLib("agent-transport")
includeSharedLib("agent-config")
includeSharedLib("knasm")
includeSharedLib("kt2dts")
includeSharedLib("kt2dts-api-sample")
includeSharedLib("kt2dts-cli")
includeSharedLib("logging-native")
includeSharedLib("logging")
includeSharedLib("test2code-api")
include("autotest-agent")
include("autotest-runtime")
include("agent-runner-common")
include("agent-runner-plugin-gradle")
include("agent-runner-plugin-maven")
include("tests-common")
include("tests-admin-stub-server")
include("tests-rest-assure-ju5")
include("tests-spock")
//TODO EPMDJ-10493 fix tests with selenium
//include("tests-selenium-v3-ju5")
//include("tests-selenium-v4-ju5")
//include("tests-cucumber-common")
//include("tests-cucumber-v4")
//include("tests-cucumber-v5")
//include("tests-cucumber-v6")
//include("tests-cucumber-v6.7")
include("tests-https-headers-common")
include("tests-https-headers-ju4")
include("tests-https-headers-ju5")
include("tests-https-headers-testng-v6.1")
include("tests-https-headers-testng-v7.4")
