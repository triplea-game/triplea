plugins {
    id("triplea-java-library")
    id("maven-publish")
}

version = System.getenv("JAR_VERSION")

dependencies {
    implementation(project(":game-app:domain-data"))
    implementation(project(":lib:feign-common"))
    implementation(project(":lib:java-extras"))
    implementation(project(":lib:websocket-client"))
    testImplementation(project(":lib:test-common"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.named(sourceSets.main.get().jarTaskName)) {
                extension = "jar"
            }
        }
    }
}
