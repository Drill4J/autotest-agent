plugins {
    java
    id("org.springframework.boot") version "2.2.6.RELEASE"
    war
}

val cucumberVersion = "4.8.0"
val springVersion = "2.2.6.RELEASE"
dependencies {
    implementation("org.springframework.boot:spring-boot-dependencies:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-web:$springVersion")
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat:$springVersion")
    implementation("com.google.code.gson:gson:2.8.6")

    testImplementation("io.github.bonigarcia:webdrivermanager:3.8.1")
    testImplementation("org.seleniumhq.selenium:selenium-java:4.0.0-alpha-4")
    testImplementation("io.cucumber:cucumber-junit:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-spring:$cucumberVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springVersion")
}
