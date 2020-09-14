
val jupiterVersion = "5.6.2"
dependencies {
    testImplementation(project(":test:https-headers"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:$jupiterVersion")
    testImplementation(kotlin("test-junit5"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}