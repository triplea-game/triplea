plugins {
    id("triplea-java-library")
}

dependencies {
    implementation(project(":game-core"))
    implementation(project(":java-extras"))
    implementation(libs.sonatype.goodies.prefs)
}

val kataDir = layout.projectDirectory.dir("kata").asFile.absolutePath
// A fixed path rather than the build directory, which a Gradle init script may relocate.
val outRoot = rootProject.layout.projectDirectory.dir("build/ai-benchmark").asFile.absolutePath

tasks.register<JavaExec>("runBenchmark") {
    group = "ai benchmark"
    description = "Plays the matching kata and writes the raw data (-Pai=, -Pkata=, -Pheap=)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "org.triplea.ai.benchmark.runner.BenchmarkMain"
    workingDir = rootDir
    maxHeapSize = providers.gradleProperty("heap").getOrElse("2g")
    args(
        "--kata-dir", kataDir,
        "--out", outRoot,
        "--ai", providers.gradleProperty("ai").getOrElse("Hard Beta"),
        "--kata", providers.gradleProperty("kata").getOrElse("*"),
    )
}

tasks.register<JavaExec>("listKata") {
    group = "ai benchmark"
    description = "Lists the kata found and their limits."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "org.triplea.ai.benchmark.runner.BenchmarkMain"
    args("--kata-dir", kataDir, "--list")
}
