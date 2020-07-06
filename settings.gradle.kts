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
}