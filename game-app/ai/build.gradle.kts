plugins {
    id("triplea-java-library")
}

dependencies {
    implementation(project(":game-core"))
    implementation(project(":java-extras"))

    testImplementation(testFixtures(project(":game-core")))
}
