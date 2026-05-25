plugins {
    id("triplea-java-library")
}

dependencies {
    implementation(project(":feign-common"))
    implementation(project(":java-extras"))
    implementation(project(":websocket-client"))
    implementation(project(":lobby-client-data"))
    testImplementation(project(":test-common"))
}
