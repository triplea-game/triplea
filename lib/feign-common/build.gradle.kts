import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("triplea-java-library")
    id("maven-publish")
}

version = System.getenv("JAR_VERSION")

dependencies {
    implementation(project(":game-app:domain-data"))
    implementation(project(":lib:java-extras"))
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
