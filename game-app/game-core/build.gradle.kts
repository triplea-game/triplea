plugins {
    id("triplea-java-library")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":domain-data"))
    implementation(project(":map-data"))
    implementation(project(":game-relay-server"))
    implementation(project(":lobby-client"))
    implementation(project(":lobby-client-data"))
    implementation(project(":java-extras"))
    implementation(project(":swing-lib"))
    implementation(project(":websocket-client"))
    implementation(project(":xml-reader"))
    implementation(libs.gson)
    testImplementation(project(":swing-lib-test-support"))
    testImplementation(project(":test-common"))
    // Build-gates the battle-calc core's island boundary; see BoundedContextBoundaryTest.
    testImplementation(libs.archunit.junit5)
    // Configures mockito to use the legacy "subclass mock maker"
    // see https://github.com/mockito/mockito/releases/tag/v5.0.0 for more information

    testFixturesImplementation(project(":java-extras"))
    testFixturesImplementation(libs.bundles.junit)
    testFixturesImplementation(libs.bundles.mockito)
    testFixturesImplementation(libs.jsr305) {
        because("This provides javax.annotations.Nullable directly, instead of relying on pulling it as a transitive dep of websockets")
    }
    testFixturesImplementation(libs.guava)
    testFixturesImplementation(libs.jetbrains.annotations)

    testFixturesCompileOnly(libs.lombok)
    testFixturesAnnotationProcessor(libs.lombok)
}

// The fuzzed battle-calc differential harness is @Tag("fuzz"): it reports expected drift and would be
// RED, so it must stay out of the blocking `check`/`./verify` gate. The default `test` task excludes
// it; the dedicated `fuzzTest` task below is the on-demand way to run it.
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("fuzz")
    }
}

tasks.register<Test>("fuzzTest") {
    description = "Runs the map-XML-fuzzed battle-calc differential drift harness (not part of check)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter(tasks.named("test"))
    useJUnitPlatform {
        includeTags("fuzz")
    }
    // The harness prints its drift summary to stdout; surface it when run directly.
    testLogging {
        showStandardStreams = true
    }
}
