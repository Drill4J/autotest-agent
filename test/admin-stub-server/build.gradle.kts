import com.github.psxpaul.task.*

plugins {
    kotlin("jvm")
    id("com.github.psxpaul.execfork") version "0.1.13"
    id("com.epam.drill.agent.runner.autotest") apply false
    id("org.jetbrains.kotlin.plugin.serialization")
}

repositories {
    mavenCentral()
}

val test2codeApiVersion: String by rootProject

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":test:commonTest"))
    implementation("com.epam.drill.plugins.test2code:api:$test2codeApiVersion")
}

val register = tasks.register("startServer", JavaExecFork::class) {
    classpath = sourceSets.main.get().runtimeClasspath
    main = "MainKt"
    jvmArgs = mutableListOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5015")
    args = mutableListOf(project.extra["adminHost"].toString(), project.extra["adminPort"].toString())
    workingDir = File("$buildDir/${project.name}")
    stopAfter = stopServer
    waitForPort = project.extra["adminPort"] as Int
    killDescendants = false
}


val stopServer = tasks.create("killProcess") {
    doFirst {
        println("Server stopped")
    }
}
