apply(plugin = "groovy")

dependencies {
    implementation(project(":test:commonTest"))
    testImplementation("org.codehaus.groovy:groovy-all:2.4.11")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4")
}

tasks.withType<Test> {
    useJUnit {
        includeCategories = setOf("UnitTest")
    }
    testLogging {
        showStandardStreams = true
    }
}
