import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("triplea-java-library")
    id("maven-publish")
}

version = System.getenv("JAR_VERSION")

description = "TripleA library for low-level helper APIs, ie: syntactic sugar"

dependencies {
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
