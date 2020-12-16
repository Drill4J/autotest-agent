apply(plugin = "groovy")

dependencies {
    testImplementation("org.codehaus.groovy:groovy-all:2.4.11")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4")
    implementation(project(":test:commonTest"))
}

tasks.withType<Test> {
    useJUnit {
        includeCategories = setOf("UnitTest")
    }
    testLogging {
        showStandardStreams = true
    }
}
