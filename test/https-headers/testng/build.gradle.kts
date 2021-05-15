dependencies {
    testImplementation(project(":test:https-headers"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-testng"))
}

tasks.named<Test>("test") {
    useTestNG()
}
