import java.net.URI
import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.util.jar.JarFile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat

@Suppress("RemoveRedundantBackticks")
plugins {
    `distribution`
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.hierynomus.license")
    id("com.github.johnrengelman.shadow")
    id("com.epam.drill.gradle.plugin.kni")
}

group = "com.epam.drill.autotest"
version = rootProject.version

val kotlinxCollectionsVersion: String by parent!!.extra
val kotlinxCoroutinesVersion: String by parent!!.extra
val kotlinxSerializationVersion: String by parent!!.extra
val atomicfuVersion: String by parent!!.extra
val javassistVersion: String by parent!!.extra
val uuidVersion: String by parent!!.extra
val javaWebsocketVersion: String by parent!!.extra
val cdtJavaClientVersion: String by parent!!.extra
val squareupOkHttpVersion: String by parent!!.extra
val nativeAgentLibName: String by parent!!.extra

val test2codeApiVersion: String by parent!!.extra

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    val configureNativeTarget: KotlinNativeTarget.() -> Unit = {
        binaries.sharedLib(nativeAgentLibName, setOf(DEBUG))
    }
    val currentPlatformTarget: KotlinMultiplatformExtension.() -> KotlinNativeTarget = {
        targets.withType<KotlinNativeTarget>()[HostManager.host.presetName]
    }
    targets {
        val jvm = jvm()
        val linuxX64 = linuxX64(configure = configureNativeTarget)
        val mingwX64 = mingwX64(configure = configureNativeTarget).apply {
            binaries.all {
                linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
            }
        }
        val macosX64 = macosX64(configure = configureNativeTarget)
        currentPlatformTarget().compilations["main"].defaultSourceSet {
            kotlin.srcDir("src/nativeMain/kotlin")
            resources.srcDir("src/nativeMain/resources")
        }
        kni {
            jvmTargets = sequenceOf(jvm)
            jvmtiAgentObjectPath = "com.epam.drill.test.agent.Agent"
            nativeCrossCompileTarget = sequenceOf(linuxX64, mingwX64, macosX64)
        }
    }
    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlin.time.ExperimentalTime")
            languageSettings.optIn("kotlinx.coroutines.DelicateCoroutinesApi")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
            languageSettings.optIn("kotlinx.serialization.InternalSerializationApi")
        }
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
                implementation("com.epam.drill.plugins.test2code:api:$test2codeApiVersion")
                implementation(project(":logger"))
                implementation(project(":kni-runtime"))
                implementation(project(":knasm"))
                implementation(project(":http-clients-instrumentation"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$kotlinxCollectionsVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")
                implementation("org.java-websocket:Java-WebSocket:$javaWebsocketVersion")
                implementation("com.github.kklisura.cdt:cdt-java-client:$cdtJavaClientVersion")
                implementation("com.squareup.okhttp3:okhttp:$squareupOkHttpVersion")
                implementation(project(":logger"))
                implementation(project(":kni-runtime"))
                implementation(project(":knasm"))
                implementation(project(":http-clients-instrumentation"))
                implementation(project(":autotest-runtime"))

                api("org.javassist:javassist:$javassistVersion")
            }
        }
        val configureNativeDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
                implementation("com.benasher44:uuid:$uuidVersion")
                implementation(project(":jvmapi"))
                implementation(project(":logger"))
                implementation(project(":kni-runtime"))
            }
        }
        val linuxX64Main by getting(configuration = configureNativeDependencies)
        val mingwX64Main by getting(configuration = configureNativeDependencies)
        val macosX64Main by getting(configuration = configureNativeDependencies)
    }
    val copyNativeClassesForTarget: TaskContainer.(KotlinNativeTarget) -> Task = {
        val copyNativeClasses:TaskProvider<Copy> = register("copyNativeClasses${it.targetName.capitalize()}", Copy::class) {
            group = "build"
            from("src/nativeMain/kotlin")
            into("src/${it.targetName}Main/kotlin/gen")
        }
        copyNativeClasses.get()
    }
    val filterOutCurrentPlatform: (KotlinNativeTarget) -> Boolean = {
        it.targetName != HostManager.host.presetName
    }
    @Suppress("UNUSED_VARIABLE")
    tasks {
        val generateNativeClasses by getting
        val jvmProcessResources by getting
        jvmProcessResources.dependsOn(generateNativeClasses)
        currentPlatformTarget().compilations["main"].compileKotlinTask.dependsOn(generateNativeClasses)
        targets.withType<KotlinNativeTarget>().filter(filterOutCurrentPlatform).forEach {
            val copyNativeClasses = copyNativeClassesForTarget(it)
            copyNativeClasses.dependsOn(generateNativeClasses)
            it.compilations["main"].compileKotlinTask.dependsOn(copyNativeClasses)
        }
        val jvmMainCompilation = kotlin.targets.withType<KotlinJvmTarget>()["jvm"].compilations["main"]
        val jvmJar by getting(Jar::class) {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
        val runtimeJar by registering(ShadowJar::class) {
            archiveFileName.set("drillRuntime.jar")
            from(jvmJar)
            relocate("kotlin", "kruntime")
            relocate("javassist", "drill.javassist")
            relocate("org.slf4j", "drill.org.slf4j")
            relocate("org.java_websocket", "drill.org.java_websocket")
            relocate("com.squareup.okhttp3", "drill.com.squareup.okhttp3")
            relocate("okio", "drill.okio")
            relocate("okhttp3", "drill.okhttp3")
        }
        val clean by getting
        val cleanGeneratedClasses by registering(Delete::class) {
            group = "build"
            delete("src/jvmMain/resources/kni-meta-info")
            delete("src/nativeMain/kotlin/kni")
            targets.withType<KotlinNativeTarget> {
                delete("src/${name}Main/kotlin/kni")
                delete("src/${name}Main/kotlin/gen")
            }
        }
        clean.dependsOn(cleanGeneratedClasses)
    }
}

distributions {
    val filterEnabledNativeTargets: (KotlinNativeTarget) -> Boolean = {
        HostManager().isEnabled(it.konanTarget)
    }
    val enabledNativeTargets = kotlin.targets.withType<KotlinNativeTarget>().filter(filterEnabledNativeTargets)
    enabledNativeTargets.forEach {
        val runtimeJarTask = tasks["runtimeJar"]
        val nativeAgentLinkTask = tasks["link${nativeAgentLibName.capitalize()}DebugShared${it.targetName.capitalize()}"]
        create(it.targetName) {
            distributionBaseName.set(it.targetName)
            contents {
                from(runtimeJarTask)
                from(nativeAgentLinkTask) {
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }
            }
        }
    }
}

@Suppress("UNUSED_VARIABLE")
license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
    val licenseFormatSources by tasks.registering(LicenseFormat::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/kni", "**/commonGenerated")
        }
    }
    val licenseCheckSources by tasks.registering(LicenseCheck::class) {
        source = fileTree("$projectDir/src").also {
            include("**/*.kt", "**/*.java", "**/*.groovy")
            exclude("**/kni", "**/commonGenerated")
        }
    }
}
