plugins {
    id("triplea-published-library")
}

dependencies {
    implementation(project(":game-app:domain-data"))
    implementation(project(":lib:feign-common"))
    implementation(project(":lib:java-extras"))
    implementation(project(":lib:websocket-client"))
    testImplementation(project(":lib:test-common"))
}
