plugins {
    id("triplea-java-library")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":battle-calc-core"))
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
        excludeTags("fuzz", "bench")
    }
}

tasks.register<Test>("fuzzTest") {
    description = "Runs the map-XML-fuzzed battle-calc differential drift harness (not part of check)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter(tasks.named("test"))
    // The alwaysHits pass rebuilds a serialize-cloned GameData oracle per scenario over the large TWW
    // map, which OOMs the default heap; raise it for this task alone.
    maxHeapSize = "6g"
    useJUnitPlatform {
        includeTags("fuzz")
    }
    // The harness prints its drift summary to stdout; surface it when run directly.
    testLogging {
        showStandardStreams = true
    }
}

// A throwaway profiling spike (@Tag("bench")): it prints ns/run, bytes/run, and engine-vs-bounded
// wall-clock rather than asserting, so it must stay out of `check`/`./verify`. Run on demand to
// gate whether the stage-1b batched simulator is worth building.
tasks.register<Test>("benchTest") {
    description = "Runs the battle-calc profiling spike (not part of check)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter(tasks.named("test"))
    useJUnitPlatform {
        includeTags("bench")
    }
    testLogging {
        showStandardStreams = true
    }
}
