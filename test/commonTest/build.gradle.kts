val test2codeApiVersion: String by rootProject
val serializationRuntimeVersion: String by rootProject


plugins {
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    api("com.epam.drill.plugins.test2code:api:$test2codeApiVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationRuntimeVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationRuntimeVersion")
    implementation(kotlin("test"))
}
