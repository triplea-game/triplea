plugins {
    id("triplea-java-library")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":domain-data"))
    implementation(project(":map-data"))
    implementation(project(":game-relay-server"))
    implementation(project(":lobby-client"))
    implementation(project(":java-extras"))
    implementation(project(":swing-lib"))
    implementation(project(":websocket-client"))
    implementation(project(":xml-reader"))
    testImplementation(project(":swing-lib-test-support"))
    testImplementation(project(":test-common"))
    // Configures mockito to use the legacy "subclass mock maker"
    // see https://github.com/mockito/mockito/releases/tag/v5.0.0 for more information

    testFixturesImplementation(project(":java-extras"))
    testFixturesImplementation(libs.bundles.junit)
    testFixturesImplementation(libs.bundles.mockito)
    testFixturesImplementation(libs.jsr305) {
        because("This provides javax.annotations.Nullable directly, instead of relying on pulling it as a transitive dep of websockets")
    }
    testFixturesImplementation(libs.guava)
    testFixturesImplementation(libs.hamcrest)
    testFixturesImplementation(libs.jetbrains.annotations)

    testFixturesCompileOnly(libs.lombok)
    testFixturesAnnotationProcessor(libs.lombok)
}
