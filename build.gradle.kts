import org.jetbrains.kotlin.konan.target.*
import java.net.*


plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.epam.drill.cross-compilation")
    id("com.epam.drill.gradle.plugin.kni")
    id("com.github.johnrengelman.shadow") version "5.1.0"
    id("com.github.hierynomus.license")
    distribution
    `maven-publish`
}

val scriptUrl: String by extra

allprojects {
    apply(from = rootProject.uri("$scriptUrl/git-version.gradle.kts"))
    apply(from = rootProject.uri("$scriptUrl/maven-repo.gradle.kts"))
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}

val kniOutputDir = "src/kni/kotlin"
val drillJvmApiLibVersion: String by rootProject
val serializationRuntimeVersion: String by rootProject
val drillLoggerVersion: String by rootProject
val drillHttpInterceptorVersion: String by rootProject
val websocketVersion: String by rootProject
val javassistVersion: String by rootProject
val klockVersion: String by rootProject
val kniVersion: String by rootProject
val uuidVersion: String by rootProject
val atomicFuVersion: String by rootProject
val collectionImmutableVersion: String by rootProject
val cdtJavaClient: String by rootProject


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
                        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationRuntimeVersion")
                        implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:$serializationRuntimeVersion")
                        implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationRuntimeVersion")
                        implementation("com.epam.drill.kni:runtime:$kniVersion")
                        implementation("com.benasher44:uuid:$uuidVersion")
                    }
                }
            }
        }
        kni {
            jvmTargets = sequenceOf(jvm("runtime"))
            additionalJavaClasses = sequenceOf()
            srcDir = kniOutputDir
            jvmtiAgentObjectPath = "com.epam.drill.test.agent.Agent"
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
        all {
            languageSettings.apply {
                useExperimentalAnnotation("kotlinx.serialization.InternalSerializationApi")
                useExperimentalAnnotation("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationRuntimeVersion")
                implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
                implementation("com.epam.drill.kni:runtime:$kniVersion")
            }
        }

        jvm("runtime") {
            compilations["main"].defaultSourceSet {
                dependencies {
                    api("org.javassist:javassist:$javassistVersion")
                    implementation("org.java-websocket:Java-WebSocket:$websocketVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationRuntimeVersion")
                    implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
                    implementation("com.soywiz.korlibs.klock:klock:$klockVersion")
                    implementation("com.epam.drill.kni:runtime:$kniVersion")
                    implementation("com.squareup.okhttp3:okhttp:3.13.1")
                    implementation("org.jetbrains.kotlinx:atomicfu:$atomicFuVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$collectionImmutableVersion")
                    implementation("com.github.kklisura.cdt:cdt-java-client:$cdtJavaClient")
                    implementation(project(":runtime"))
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
    relocate("com.squareup.okhttp3", "drill.com.squareup.okhttp3")
    relocate("okhttp3", "drill.okhttp3")
    relocate("okio", "drill.okio")
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
    publications {
        nativeTargets.filter {
            HostManager().isEnabled(it.konanTarget)
        }.forEach {
            create<MavenPublication>("${it.name}Zip") {
                artifactId = "$libName-${it.name}"
                artifact(tasks["${it.name}DistZip"])
            }
        }
    }
}

tasks {
    val generateNativeClasses by getting {}
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
        dependsOn(generateNativeClasses)
    }
    val cleanExtraData by registering(Delete::class) {
        group = "build"
        delete(kniOutputDir)
    }

    clean {
        dependsOn(cleanExtraData)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.time.ExperimentalTime"
}

val licenseFormatSettings by tasks.registering(com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir).also {
        include("**/*.kt", "**/*.java", "**/*.groovy")
        exclude("**/.idea")
    }.asFileTree
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
}

tasks["licenseFormat"].dependsOn(licenseFormatSettings)
