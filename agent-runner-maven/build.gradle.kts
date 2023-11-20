plugins {
    kotlin("jvm")
    `maven-publish`
}

val kotlinVersion: String by extra

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.apache.maven:maven-core:3.8.1")
    implementation("org.apache.maven:maven-plugin-api:3.8.1")
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.1")
    implementation("org.twdata.maven:mojo-executor:2.3.2")
    implementation("org.apache.maven.plugins:maven-surefire-plugin:2.22.2")
    implementation(project(":agent-runner-common"))
}

@Suppress("UNUSED_VARIABLE")
tasks {
    val sourcesJar by registering(Jar::class) {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
    val install by registering(Exec::class) {
        val isWindows = System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0
        val args = if (isWindows) arrayOf("cmd", "/c", "mvnw.cmd") else arrayOf("sh", "./mvnw")
        commandLine(*args, "install", "-Ddrill.plugin.version=$version", "-Dkotlin.version=$kotlinVersion")
        workingDir(project.projectDir)
        standardOutput = System.out
    }
    publish.get().dependsOn(install)
    publishToMavenLocal.get().dependsOn(install)
}


publishing {
    publications {
        create<MavenPublication>("drill-maven") {
            groupId = "com.epam.drill.agent.runner"
            artifactId = "maven"
            artifact(file("target/maven-$version.jar"))
            artifact(tasks["sourcesJar"])
            pom.withXml {
                asNode().apply {
                    appendNode("dependencies").apply {
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.jetbrains.kotlin")
                            appendNode("artifactId", "kotlin-stdlib")
                            appendNode("version", kotlinVersion)
                        }
                    }
                }
            }
        }
    }
}
