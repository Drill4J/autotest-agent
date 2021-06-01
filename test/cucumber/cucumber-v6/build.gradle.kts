val cucumberVersion = "6.0.0"

dependencies {
    testImplementation("io.cucumber:cucumber-testng:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
}

tasks.withType<Test> {
    useJUnit()
}
