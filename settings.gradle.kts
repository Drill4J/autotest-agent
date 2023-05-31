rootProject.name = "autotest-agent"

pluginManagement {
    val kotlinVersion: String by extra
    val licenseVersion: String by extra
    val grgitVersion: String by extra
    val shadowPluginVersion: String by extra
    val psxpaulExecforkVersion: String by extra
    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.noarg") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.ajoberstar.grgit") version grgitVersion
        id("com.github.hierynomus.license") version licenseVersion
        id("com.github.johnrengelman.shadow") version shadowPluginVersion
        id("com.github.psxpaul.execfork") version psxpaulExecforkVersion
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

includeSharedLib("http-clients-instrumentation")
includeSharedLib("jvmapi")
includeSharedLib("knasm")
includeSharedLib("kni-runtime")
includeSharedLib("kt2dts")
includeSharedLib("kt2dts-api-sample")
includeSharedLib("kt2dts-cli")
includeSharedLib("logger")
includeSharedLib("logger-api")
includeSharedLib("logger-test-agent")
includeSharedLib("runtime")
includeSharedLib("test2code-api")
include("autotest-agent")
include("autotest-runtime")
include("tests-common")
include("tests-admin-stub-server")
include("tests-rest-assure-ju5")
include("tests-spock")
