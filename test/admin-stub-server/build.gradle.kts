import com.github.jengelman.gradle.plugins.shadow.tasks.*
import java.net.*

plugins {
    kotlin("jvm")
    id("com.epam.drill.agent.runner.autotest") apply false
    id("com.github.johnrengelman.shadow")
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


val fatJar = tasks.withType<ShadowJar> {
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    archiveFileName.set("${project.name}.jar")
}

var serverBackgroundProcess: Process? = null

val register = tasks.register("startServer") {
    dependsOn(fatJar)
    doFirst {
        serverBackgroundProcess?.destroy()
        val jarDir = projectDir
            .resolve("build")
            .resolve("libs")
        if (jarDir.exists()) {
            serverBackgroundProcess = ProcessBuilder()
                .directory(jarDir)
                .command("java",
                    "-jar",
                    "${project.name}.jar",
                    project.extra["adminHost"].toString(),
                    project.extra["adminPort"].toString()
                ).start()
            waitForServerStart()
            println("Server started on host: ${project.extra["adminHost"]} port: ${project.extra["adminPort"]}")
        }
    }
}

val stopServer = tasks.register("stopServer") {
    doFirst {
        serverBackgroundProcess?.apply {
            destroy()
            println("Server stopped")
        }
    }
}

fun waitForServerStart(
    port: Int = project.extra["adminPort"] as Int,
    process: Process? = serverBackgroundProcess,
    timeout: Long = 70,
    unit: TimeUnit = TimeUnit.SECONDS,
) {
    val millisToWait: Long = unit.toMillis(timeout)
    val waitUntil: Long = System.currentTimeMillis() + millisToWait

    while (System.currentTimeMillis() < waitUntil) {
        Thread.sleep(100)
        if (process == null || !process.isAlive) throw GradleException("Process died before port $port was opened")
        if (isPortOpen(port)) return
    }

    throw GradleException("Timed out waiting for port $port to be opened")
}

fun isPortOpen(port: Int): Boolean {
    Socket().use {
        val inetAddress: InetAddress = InetAddress.getByName(project.extra["adminHost"].toString())
        val socketAddress = InetSocketAddress(inetAddress, port)
        return try {
            it.connect(socketAddress)
            true
        } catch (e: ConnectException) {
            false
        }
    }
}
