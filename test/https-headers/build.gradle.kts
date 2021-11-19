allprojects {
    dependencies {
        implementation(project(":test:commonTest"))
        //TODO move to commonTest module
        api(project(":")) { isTransitive = false }
    }
}

dependencies {
    implementation(kotlin("test"))
}
