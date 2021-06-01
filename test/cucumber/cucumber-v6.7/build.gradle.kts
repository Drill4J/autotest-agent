val cucumberVersion = "6.7.0"

dependencies {
    testImplementation("io.cucumber:cucumber-testng:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
}

tasks.withType<Test> {
    useJUnit()
}
