plugins {
    kotlin("jvm")
    id("com.epam.drill.agent.runner.autotest") apply false
    id("com.github.psxpaul.execfork") version "0.2.0"
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

val jar: org.gradle.jvm.tasks.Jar by tasks

val register = tasks.register("startServer", com.github.psxpaul.task.JavaExecFork::class) {
    dependsOn(jar)
    classpath = sourceSets.main.get().runtimeClasspath
    main = "MainKt"
    jvmArgs = mutableListOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5015")
    args = mutableListOf(project.extra["adminHost"].toString(), project.extra["adminPort"].toString())
    workingDir = jar.archiveFile.get().asFile.parentFile
    stopAfter = stopServer
    waitForPort = project.extra["adminPort"] as Int
    killDescendants = false
}

val stopServer = tasks.register("stopServer") {
    doFirst {
        println("Server stopped")
    }
}
