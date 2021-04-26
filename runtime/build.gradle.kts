plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    `maven-publish`
}
val kniVersion: String by rootProject


repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}
dependencies {
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
    publications{
        create<MavenPublication>("maven") {
            artifact(agentShadow.get())
        }
    }
}
