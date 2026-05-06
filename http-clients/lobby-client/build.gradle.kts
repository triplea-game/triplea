plugins {
    id("triplea-published-library")
}

dependencies {
    implementation(project(":lobby-client-data"))
    implementation(project(":domain-data"))
    implementation(project(":feign-common"))
    implementation(project(":java-extras"))
    implementation(project(":websocket-client"))
    testImplementation(project(":test-common"))
}
