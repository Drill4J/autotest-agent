rootProject.name = "autotest-agent"
include(":runtime")
include(":test")
include(":test:commonTest")
include(":test:https-headers")
include(":test:https-headers:ju5")
include(":test:https-headers:ju4")
include(":test:https-headers:testng")
include(":test:https-headers:testng:testng-v6.1")
include(":test:https-headers:testng:testng-v7.4")
include(":test:selenium:selenium-v3-ju5")
include(":test:selenium:selenium-v4-ju5")
include(":test:rest-assure-ju5")

include(":test:spock")
include(":test:cucumber:cucumber-v4")
include(":test:cucumber:cucumber-v5")
include(":test:cucumber:cucumber-v6")
include(":test:cucumber:cucumber-v6.7")

include(":test:admin-stub-server")
pluginManagement {
    repositories {
        mavenLocal()
        maven(url = "https://oss.jfrog.org/oss-release-local")
        maven(url = "https://drill4j.jfrog.io/artifactory/drill")
        maven(url = "https://oss.sonatype.org/service/local/repo_groups/staging/content")
        gradlePluginPortal()
    }
    val kotlinVersion: String by extra
    val kniVersion: String by extra
    val shadowJarPluginVersion: String by extra
    val licenseVersion: String by extra
    plugins {
        kotlin("multiplatform") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        id("com.epam.drill.gradle.plugin.kni") version kniVersion
        id("com.github.hierynomus.license") version licenseVersion
        id("com.github.johnrengelman.shadow") version shadowJarPluginVersion
    }
}
