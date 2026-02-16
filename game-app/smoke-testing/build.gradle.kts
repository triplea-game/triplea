import de.undercouch.gradle.tasks.download.Download

plugins {
    id("triplea-java-library")
    alias(libs.plugins.download)
}

dependencies {
    implementation(project(":game-app:domain-data"))
    implementation(project(":game-app:game-core"))
    testImplementation(project(":game-app:game-headless"))
    testImplementation(project(":lib:java-extras"))
    testImplementation(project(":lib:test-common"))
}

tasks.named<Test>("test") {
    // AiGameTest is memory intensive due to ConcurrentBattleCalculator threads deserializing GameData concurrently.
    maxHeapSize = "2G"
}

// Gather all URLs in the same game list file
val downloadURLs = mutableListOf<String>()
val saveGamesFile = project.layout.projectDirectory.file("save-game-list.txt").asFile
saveGamesFile.readLines().forEachIndexed { _, path ->
    if (!path.startsWith("#")) {
        downloadURLs.add(path)
    }
}

// Create a download task for the URLs
val downloadTask = tasks.register<Download>("downloadSaveGames") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Downloads save game files"

    src(downloadURLs)
    dest(project.layout.buildDirectory.dir("downloads/save-games"))
    overwrite(false)
    onlyIfModified(true)
    quiet(true)
}

// And wire test resource processing as a dependant on the download task
tasks.named<ProcessResources>("processTestResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(downloadTask) {
        into("save-games")
    }
}
