rootProject.name = "autotest-agent"
include (":runtime")
pluginManagement {
    repositories {
        mavenLocal()
        maven(url = "http://oss.jfrog.org/oss-release-local")
        gradlePluginPortal()
    }
}