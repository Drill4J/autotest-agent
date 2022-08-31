import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.net.*


plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.epam.drill.gradle.plugin.kni")
    id("com.github.johnrengelman.shadow")
    id("com.github.hierynomus.license")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
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
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
    maven(url = "https://drill4j.jfrog.io/artifactory/drill")
}

val kniOutputDir = "src/kni/kotlin"
val drillJvmApiLibVersion: String by rootProject
val serializationRuntimeVersion: String by rootProject
val drillLoggerVersion: String by rootProject
val websocketVersion: String by rootProject
val kniVersion: String by rootProject
val uuidVersion: String by rootProject
val atomicFuVersion: String by rootProject
val collectionImmutableVersion: String by rootProject
val cdtJavaClient: String by rootProject
val javassistVersion: String by rootProject
val knasmVersion: String by rootProject
val httpClientInstrumentVersion: String by rootProject
val test2codeApiVersion: String by rootProject
val coroutinesVersion: String by rootProject


val libName = "autoTestAgent"

val currentPlatformName = HostManager.host.presetName

kotlin {
    targets {
        val nativeTargets = sequenceOf(
            linuxX64(),
            macosX64(),
            mingwX64 { binaries.all { linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock") } }
        )
        nativeTargets.forEach { target ->
            if (currentPlatformName == target.name) {
                target.compilations["main"].setCommonSources()
            }
            target.binaries {
                sharedLib(libName, setOf(DEBUG))
            }
        }
        jvm("runtime") {
            compilations["main"].defaultSourceSet {
                dependencies {
                    api("org.javassist:javassist:$javassistVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationRuntimeVersion")
                    implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
                    implementation("com.epam.drill.kni:runtime:$kniVersion")
                    //todo EPMDJ-10494 remove
                    //implementation("com.squareup.okhttp3:okhttp:3.13.1")
                    implementation("org.jetbrains.kotlinx:atomicfu:$atomicFuVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$collectionImmutableVersion")
                    implementation("com.epam.drill:http-clients-instrumentation:$httpClientInstrumentVersion")
                    implementation("com.epam.drill.knasm:knasm:$knasmVersion")
                    implementation(project(":runtime"))
                }
            }
        }
        kni {
            jvmTargets = sequenceOf(jvm("runtime"))
            additionalJavaClasses = sequenceOf()
            jvmtiAgentObjectPath = "com.epam.drill.test.agent.Agent"
            nativeCrossCompileTarget = nativeTargets
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlinx.serialization.InternalSerializationApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationRuntimeVersion")
                implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
                implementation("com.epam.drill.kni:runtime:$kniVersion")
                implementation("com.epam.drill.plugins.test2code:api:$test2codeApiVersion")
                implementation("com.epam.drill:http-clients-instrumentation:$httpClientInstrumentVersion")
                implementation("com.epam.drill.knasm:knasm:$knasmVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core") {
                    version { strictly("$coroutinesVersion-native-mt") }
                }
            }
        }
        //TODO EPMDJ-8696 Rename to commonNative
        val commonNativeDependenciesOnly by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("com.epam.drill:jvmapi:$drillJvmApiLibVersion")
                implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationRuntimeVersion")
                implementation("com.epam.drill.kni:runtime:$kniVersion")
                implementation("com.benasher44:uuid:$uuidVersion")
            }
        }
        val linuxX64Main by getting {
            dependsOn(commonNativeDependenciesOnly)
        }
        val mingwX64Main by getting {
            dependsOn(commonNativeDependenciesOnly)
        }
        val macosX64Main by getting {
            dependsOn(commonNativeDependenciesOnly)
        }

    }
}
val nativeTargets = kotlin.targets.filterIsInstance<KotlinNativeTarget>()


val runtimeJar by tasks.getting(Jar::class) {
    from(provider {
        kotlin.jvm("runtime").compilations["main"].compileDependencyFiles.map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
    //todo EPMDJ-10494 remove
    //relocate("com.squareup.okhttp3", "drill.com.squareup.okhttp3")
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
    //TODO EPMDJ-8696 remove copy
    val copy = nativeTargets.filter { it.name != currentPlatformName }.map {
        register<Copy>("copy for ${it.name}") {
            from(file("src/commonNative/kotlin"))
            into(file("src/${it.name}Main/kotlin/gen"))
        }
    }
    val copyCommon by registering(DefaultTask::class) {
        group = "build"
        copy.forEach { dependsOn(it) }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
        dependsOn(copyCommon)
        dependsOn(generateNativeClasses)
    }
    val cleanExtraData by registering(Delete::class) {
        group = "build"
        nativeTargets.forEach {
            val path = "src/${it.name}Main/kotlin/"
            delete(file("${path}kni"), file("${path}gen"))
        }
    }

    clean {
        dependsOn(cleanExtraData)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.DelicateCoroutinesApi"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.DelicateCoroutinesApi"
}

val licenseFormatSettings by tasks.registering(com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir).also {
        include("**/*.kt", "**/*.java", "**/*.groovy")
        exclude("**/.idea")
    }.asFileTree
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
}

tasks["licenseFormat"].dependsOn(licenseFormatSettings)

//TODO EPMDJ-8696 remove
fun KotlinNativeCompilation.setCommonSources(modulePath: String = "src/commonNative") {
    defaultSourceSet {
        kotlin.srcDir(file("${modulePath}/kotlin"))
        resources.setSrcDirs(listOf("${modulePath}/resources"))
    }
}
