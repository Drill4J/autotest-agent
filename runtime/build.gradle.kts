plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    `maven-publish`
}
val kniVersion: String by rootProject

apply(from = "https://raw.githubusercontent.com/Drill4J/build-scripts/master/git-version.gradle.kts")

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.epam.drill.kni:runtime:$kniVersion")
}

val jar:org.gradle.jvm.tasks.Jar by tasks
val agentShadow by tasks.registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    from(jar)
    archiveFileName.set("drillRuntime.jar")
    relocate("kotlin", "kruntime")
    relocate("javassist", "drill.javassist")
    relocate("org.java_websocket", "drill.org.java_websocket")
    relocate("org.slf4j", "drill.org.slf4j")
}
publishing {
    repositories {
        maven {

            url = uri("http://oss.jfrog.org/oss-release-local")
            credentials {
                username =
                    if (project.hasProperty("bintrayUser"))
                        project.property("bintrayUser").toString()
                    else System.getenv("BINTRAY_USER")
                password =
                    if (project.hasProperty("bintrayApiKey"))
                        project.property("bintrayApiKey").toString()
                    else System.getenv("BINTRAY_API_KEY")
            }
        }
    }
    publications{
        create<MavenPublication>("maven") {
            artifact(agentShadow.get())
        }
    }
}