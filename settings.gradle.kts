rootProject.name = "autotest-agent"
include(":test")
include(":test:https-headers")
include(":test:selenium-v3")
include(":test:selenium-v4")
include(":test:rest-assure")
pluginManagement {
    repositories {
        mavenLocal()
        maven(url = "http://oss.jfrog.org/oss-release-local")
        gradlePluginPortal()
    }
    val kotlinVersion: String by extra
    val drillGradlePluginVersion: String by extra
    val drillGradleAutotestPluginVersion: String by extra
    plugins {
        kotlin("multiplatform") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        id("com.epam.drill.cross-compilation") version drillGradlePluginVersion
        id("com.epam.drill.agent.runner.autotest") version drillGradleAutotestPluginVersion
    }
}