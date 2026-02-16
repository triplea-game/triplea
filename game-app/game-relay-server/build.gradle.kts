plugins {
    id("triplea-java-library")
}

dependencies {
    implementation(project(":websocket-client"))
    implementation(project(":websocket-server"))
    testImplementation(project(":test-common"))
}
