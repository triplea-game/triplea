plugins {
    id("triplea-java-library")
}

dependencies {
    implementation(project(":game-app:game-core"))
    implementation(project(":lib:java-extras"))

    testImplementation(testFixtures(project(":game-app:game-core")))
}
