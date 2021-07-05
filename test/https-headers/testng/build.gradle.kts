subprojects {
    tasks.named<Test>("test") {
        useTestNG()
    }
}

