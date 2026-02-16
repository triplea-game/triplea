plugins {
    id("triplea-java-library")
    id("maven-publish")
}

version = System.getenv("JAR_VERSION")

dependencies {
    implementation(project(":domain-data"))
    implementation(project(":feign-common"))
    implementation(project(":java-extras"))
    implementation(project(":websocket-client"))
    testImplementation(project(":test-common"))
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
