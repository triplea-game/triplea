plugins {
    id("triplea-java-library")
}

dependencies {
    implementation(project(":domain-data"))
    implementation(project(":java-extras"))
    testImplementation(project(":test-common"))
}
