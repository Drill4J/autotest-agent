import  org.apache.tools.ant.taskdefs.condition.Os

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.3.70"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.70"
    id("com.epam.drill.cross-compilation") version "0.16.0"
    id("com.epam.drill.agent.runner.autotest") version "0.1.0"
    distribution
    `maven-publish`
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}

val drillJvmApiLibVersion: String by rootProject
val serializationRuntimeVersion: String by rootProject
val drillLogger: String by rootProject
val drillHttpInterceptorVersion: String by rootProject
val transportVersion: String by rootProject

val libName = "autoTestAgent"
kotlin {

    targets {
        crossCompilation {
            common {
                dependencies {
                    implementation("com.epam.drill:jvmapi-native:$drillJvmApiLibVersion")
                    implementation("com.epam.drill.interceptor:http:$drillHttpInterceptorVersion")
                    implementation("com.epam.drill.logger:logger:$drillLogger")
                    implementation("com.epam.drill.transport:core:$transportVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationRuntimeVersion")
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
            }
        }
        val jupiterVersion = "5.4.2"
        val gsonVersion = "2.8.5"
        val restAssuredVersion = "4.0.0"
        jvm {
            compilations["test"].defaultSourceSet {
                dependencies {
                    implementation("com.google.code.gson:gson:$gsonVersion")
                    implementation("org.junit.jupiter:junit-jupiter:$jupiterVersion")
                    implementation("io.rest-assured:rest-assured:$restAssuredVersion")
                    implementation(kotlin("stdlib-jdk8"))
                }
            }
        }
    }
}

val runtimeProject = project(":runtime")
val shadowJar = provider { runtimeProject.tasks.getByPath("shadowJar") }
val jvmMainClasses by tasks.getting {
    dependsOn(shadowJar)
}

val nativeTargets = kotlin.targets.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()
distributions {
    nativeTargets.forEach {
        val name = it.name
        create(name) {
            distributionBaseName.set(name)
            contents {
                from(shadowJar)
                from(tasks.getByPath("link${libName.capitalize()}DebugShared${name.capitalize()}"))
            }
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
            org.jetbrains.kotlin.konan.target.HostManager()
                .isEnabled(it.konanTarget)
        }.forEach {
            create<MavenPublication>("${it.name}Zip") {
                artifactId = "$libName-${it.name}"
                artifact(tasks["${it.name}DistZip"])
            }
        }
    }
}

val presetName: String =
    when {
        Os.isFamily(Os.FAMILY_MAC) -> "macosX64"
        Os.isFamily(Os.FAMILY_UNIX) -> "linuxX64"
        Os.isFamily(Os.FAMILY_WINDOWS) -> "mingwX64"
        else -> throw RuntimeException("Target ${System.getProperty("os.name")} is not supported")
    }

tasks.named<org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest>("jvmTest") {
    dependsOn(tasks.getByPath("link${libName.capitalize()}DebugShared${presetName.capitalize()}"))
    dependsOn(tasks.getByPath("install${presetName.capitalize()}Dist"))
    useJUnitPlatform()
}

val targetFromPreset = (kotlin.targets[presetName]) as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

drill {
    runtimePath = file("./build/install/$presetName")
    agentPath =
        targetFromPreset
            .binaries
            .findSharedLib(libName, org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG)!!
            .outputFile.toPath().toFile()
    agentId = "Petclinic"
    adminHost = "localhost"
    adminPort = 8090
    plugins += "junit"
    debug = true
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
}
