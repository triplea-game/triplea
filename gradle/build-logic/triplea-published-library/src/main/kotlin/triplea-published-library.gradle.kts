plugins {
    id("triplea-java-library")
    id("maven-publish")
}

version = System.getenv("JAR_VERSION")

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/triplea-game/triplea")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.named(sourceSets.main.get().jarTaskName)) {
                extension = "jar"
            }
        }
    }
}
