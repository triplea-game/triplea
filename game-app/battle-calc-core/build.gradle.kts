plugins {
    id("triplea-java-library")
}

description =
    "GameData-free battle-calculator core: pure combat model, seams, reference and vectorized " +
        "simulators. Depends on no engine or game-core type so the compiler enforces the island boundary."

dependencies {
    // Build-gates the island boundary from inside the module; see BoundedContextBoundaryTest.
    testImplementation(libs.archunit.junit5)
}
