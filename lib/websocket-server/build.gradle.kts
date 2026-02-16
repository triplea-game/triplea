plugins {
    id("triplea-java-library")
}

dependencies {
    implementation(project(":feign-common"))
    implementation(project(":java-extras"))
    implementation(project(":websocket-client"))
    testImplementation(project(":test-common"))
}
