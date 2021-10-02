plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    `maven-publish`
}
val kniVersion: String by rootProject


repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://drill4j.jfrog.io/artifactory/drill")
}
dependencies {
    implementation("com.epam.drill.kni:runtime:$kniVersion")
}

val jar: org.gradle.jvm.tasks.Jar by tasks

//TODO https://youtrack.jetbrains.com/issue/KT-46165
jar.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

val agentShadow by tasks.registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    from(jar)
    archiveFileName.set("drillRuntime.jar")
    relocate("kotlin", "kruntime")
    relocate("javassist", "drill.javassist")
    relocate("org.java_websocket", "drill.org.java_websocket")
    relocate("org.slf4j", "drill.org.slf4j")
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(agentShadow.get())
        }
    }
}
