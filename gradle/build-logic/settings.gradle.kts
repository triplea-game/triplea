@file:Suppress("UnstableApiUsage") // For repository declarations in settings

import org.gradle.api.initialization.resolve.RepositoriesMode

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"

include("failure-summary-plugin", "triplea-test-conventions", "triplea-base-project",
    "triplea-java-library", "triplea-java-application", "triplea-published-library", )
