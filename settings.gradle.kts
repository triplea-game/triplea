@file:Suppress("UnstableApiUsage") // For repository declarations in settings

import org.gradle.api.initialization.resolve.RepositoriesMode
import java.net.URI

pluginManagement {
    includeBuild("gradle/build-logic")
}

plugins {
    id("org.triplea.failure-summary-plugin")
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/triplea-game/triplea")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

rootProject.name = "triplea"

include(":ai")
project(":ai").projectDir = file("game-app/ai")
include(":domain-data")
project(":domain-data").projectDir = file("game-app/domain-data")
include(":game-core")
project(":game-core").projectDir = file("game-app/game-core")
include(":game-headed")
project(":game-headed").projectDir = file("game-app/game-headed")
include(":game-headless")
project(":game-headless").projectDir = file("game-app/game-headless")
include(":game-relay-server")
project(":game-relay-server").projectDir = file("game-app/game-relay-server")
include(":map-data")
project(":map-data").projectDir = file("game-app/map-data")
include(":smoke-testing")
project(":smoke-testing").projectDir = file("game-app/smoke-testing")

include(":lobby-client")
project(":lobby-client").projectDir = file("http-clients/lobby-client")
include(":lobby-client-data")
project(":lobby-client-data").projectDir = file("http-clients/lobby-client-data")


include(":feign-common")
project(":feign-common").projectDir = file("lib/feign-common")
include(":java-extras")
project(":java-extras").projectDir = file("lib/java-extras")
include(":swing-lib")
project(":swing-lib").projectDir = file("lib/swing-lib")
include(":swing-lib-test-support")
project(":swing-lib-test-support").projectDir = file("lib/swing-lib-test-support")
include(":test-common")
project(":test-common").projectDir = file("lib/test-common")
include(":websocket-client")
project(":websocket-client").projectDir = file("lib/websocket-client")
include(":websocket-server")
project(":websocket-server").projectDir = file("lib/websocket-server")
include(":xml-reader")
project(":xml-reader").projectDir = file("lib/xml-reader")
