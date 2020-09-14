val cucumberVersion = "4.8.0"

dependencies {
    testImplementation("io.github.bonigarcia:webdrivermanager:3.8.1")
    testImplementation("org.seleniumhq.selenium:selenium-java:4.0.0-alpha-4")
    testImplementation("io.cucumber:cucumber-testng:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
}

tasks.withType<Test> {
    useJUnit()
}
