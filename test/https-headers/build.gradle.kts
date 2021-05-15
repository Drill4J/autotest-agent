allprojects {
    dependencies {
        implementation(project(":test:commonTest"))
        //TODO move to commonTest module
        implementation(project(":")) { isTransitive = false }
    }
}

dependencies {
    implementation(kotlin("test"))
}
