import org.jetbrains.kotlin.konan.target.*


plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.epam.drill.cross-compilation")
    id("com.github.johnrengelman.shadow") version "5.1.0"
    distribution
    `maven-publish`
}

apply(from = "https://raw.githubusercontent.com/Drill4J/build-scripts/master/git-version.gradle.kts")

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:1.3.5")).with(module("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:1.3.5-native-mt"))
    }
}

val drillJvmApiLibVersion: String by rootProject
val serializationRuntimeVersion: String by rootProject
val drillLoggerVersion: String by rootProject
val drillHttpInterceptorVersion: String by rootProject
val transportVersion: String by rootProject
val websocketVersion: String by rootProject
val javassistVersion: String by rootProject
val klockVersion: String by rootProject

val libName = "autoTestAgent"
kotlin {

    targets {
        crossCompilation {
            common {
                defaultSourceSet {
                    dependsOn(sourceSets.commonMain.get())
                    dependencies {
                        implementation("com.epam.drill:jvmapi:$drillJvmApiLibVersion")
                        implementation("com.epam.drill.interceptor:http:$drillHttpInterceptorVersion")
                        implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
                        implementation("com.epam.drill.transport:core:$transportVersion")
                        implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationRuntimeVersion")
                        implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties-native:$serializationRuntimeVersion")
                        implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-native:$serializationRuntimeVersion")
                    }
                }
            }
        }
        sequenceOf(
            linuxX64(),
            macosX64(),
            mingwX64 { binaries.all { linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock") } }
        ).forEach {
            it.binaries {
                sharedLib(libName, setOf(DEBUG))
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationRuntimeVersion")
                implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
            }
        }

        jvm("runtime") {
            compilations["main"].defaultSourceSet {
                languageSettings.apply {
                    useExperimentalAnnotation("kotlinx.serialization.UnstableDefault")
                }
                dependencies {
                    implementation(kotlin("stdlib-jdk8"))
                    api("org.javassist:javassist:$javassistVersion")
                    implementation("org.java-websocket:Java-WebSocket:$websocketVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                    implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
                    implementation("com.soywiz.korlibs.klock:klock-jvm:$klockVersion")
                }
            }
        }

    }
}

val nativeTargets = kotlin.targets.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()

val runtimeJar by tasks.getting(Jar::class) {
    from(provider {
        kotlin.jvm("runtime").compilations["main"].compileDependencyFiles.map { if (it.isDirectory) it else zipTree(it) }
    })
}

val resourceDir = buildDir
    .resolve("resources")
    .resolve("main")

val agentShadow by tasks.registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    val extensionDistZip = tasks.getByPath("extensionDistZip")
    dependsOn(extensionDistZip)
    doFirst {
        val firefoxAddon = resourceDir.apply { mkdirs() }.resolve("header-transmitter.xpi")
        extensionDistZip.outputs.files.singleFile.renameTo(firefoxAddon)
    }
    from(runtimeJar)
    from(resourceDir)
    archiveFileName.set("drillRuntime.jar")
    relocate("kotlin", "kruntime")
    relocate("javassist", "drill.javassist")
    relocate("org.java_websocket", "drill.org.java_websocket")
    relocate("org.slf4j", "drill.org.slf4j")
}

distributions {
    nativeTargets.forEach {
        val name = it.name
        create(name) {
            distributionBaseName.set(name)
            contents {
                from(agentShadow)
                from(tasks.getByPath("link${libName.capitalize()}DebugShared${name.capitalize()}")) {
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }
            }
        }
    }
    create("extension") {
        distributionBaseName.set("extension")
        contents {
            from(file("drill-header-transmitter"))
            eachFile(object : Action<FileCopyDetails> {
                override fun execute(fcp: FileCopyDetails) {
                    fcp.relativePath = RelativePath(
                        true, fcp.relativePath.pathString
                            .replace("extension-$version/", "")
                            .replace("extension/", "")
                    )
                }
            })
        }
    }
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
    publications {
        kotlin.targets.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().filter {
            HostManager().isEnabled(it.konanTarget)
        }.forEach {
            create<MavenPublication>("${it.name}Zip") {
                artifactId = "$libName-${it.name}"
                artifact(tasks["${it.name}DistZip"])
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
}
