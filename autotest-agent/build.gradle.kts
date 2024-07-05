import java.net.URI
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
}

group = "com.epam.drill.autotest"
version = rootProject.version

val kotlinxCollectionsVersion: String by parent!!.extra
val kotlinxCoroutinesVersion: String by parent!!.extra
val kotlinxSerializationVersion: String by parent!!.extra
val atomicfuVersion: String by parent!!.extra
val javassistVersion: String by parent!!.extra
val uuidVersion: String by parent!!.extra
val aesyDatasizeVersion: String by parent!!.extra
val nativeAgentLibName: String by parent!!.extra
val macosLd64 : String by parent!!.extra

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
        jvm()
        linuxX64(configure = configureNativeTarget)
        mingwX64(configure = configureNativeTarget).apply {
            binaries.all {
                linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
            }
        }
        macosX64(configure = configureNativeTarget).apply {
            if (macosLd64.toBoolean()) {
                binaries.all {
                    linkerOpts("-ld64")
                }
            }
        }
        currentPlatformTarget().compilations["main"].defaultSourceSet {
            kotlin.srcDir("src/nativeMain/kotlin")
            resources.srcDir("src/nativeMain/resources")
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
                implementation(project(":logging"))
                implementation(project(":common"))
                implementation(project(":agent-instrumentation"))
                implementation(project(":agent-config"))
                implementation(project(":test2code-api"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$kotlinxCollectionsVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                implementation("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")
                implementation("org.javassist:javassist:$javassistVersion")
                implementation("io.aesy:datasize:$aesyDatasizeVersion")
                implementation("com.benasher44:uuid:$uuidVersion")
                implementation(project(":agent-transport"))
                implementation(project(":knasm"))
            }
        }
        val configureNativeDependencies: KotlinSourceSet.() -> Unit = {
            dependencies {
                implementation(project(":jvmapi"))
                implementation(project(":konform"))
                implementation("com.benasher44:uuid:$uuidVersion")
            }
        }
        val linuxX64Main by getting(configuration = configureNativeDependencies)
        val mingwX64Main by getting(configuration = configureNativeDependencies)
        val macosX64Main by getting(configuration = configureNativeDependencies)
        mingwX64Main.dependencies {
            implementation(project(":logging-native"))
        }
        macosX64Main.dependencies {
            implementation(project(":logging-native"))
        }
    }
    val copyNativeClassesForTarget: TaskContainer.(KotlinNativeTarget) -> Task = {
        val copyNativeClasses: TaskProvider<Copy> =
            register("copyNativeClasses${it.targetName.capitalize()}", Copy::class) {
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
        targets.withType<KotlinNativeTarget>().filter(filterOutCurrentPlatform).forEach {
            val copyNativeClasses = copyNativeClassesForTarget(it)
            it.compilations["main"].compileKotlinTask.dependsOn(copyNativeClasses)
        }
        val jvmMainCompilation = kotlin.targets.withType<KotlinJvmTarget>()["jvm"].compilations["main"]
        val extensionZip by registering(Zip::class) {
            group = "shadow"
            archiveFileName.set("header-transmitter.xpi")
            destinationDirectory.set(temporaryDir)
            from(rootDir.resolve("drill-header-transmitter"))
        }
        val relocatePackages = setOf(
            "javax.validation",
            "javassist",
            "ch.qos.logback",
            "io.aesy.datasize",
            "com.alibaba",
            "com.benasher44",
            "io.ktor",
            "net.bytebuddy",
            "org.objectweb.asm",
            "org.slf4j",
            "org.apache",
            "org.intellij.lang.annotations",
            "org.jetbrains.annotations",
            "org.petitparser",
            "mu",
        )
        val runtimeJar by registering(ShadowJar::class) {
            group = "shadow"
            isZip64 = true
            archiveFileName.set("drill-runtime.jar")
            from(jvmMainCompilation.output, jvmMainCompilation.runtimeDependencyFiles)
            from(extensionZip)
            relocate("kotlin", "kruntime")
            relocate("kotlinx", "kruntimex")
            relocatePackages.forEach {
                relocate(it, "${project.group}.shadow.$it")
            }
            dependencies {
                exclude("/META-INF/services/javax.servlet.ServletContainerInitializer")
                exclude("/module-info.class")
                exclude("/ch/qos/logback/classic/servlet/*")
                exclude("/target/classes/*")
            }
        }
        val clean by getting
        val cleanGeneratedClasses by registering(Delete::class) {
            group = "build"
            targets.withType<KotlinNativeTarget> {
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
        val nativeAgentLinkTask =
            tasks["link${nativeAgentLibName.capitalize()}DebugShared${it.targetName.capitalize()}"]
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
