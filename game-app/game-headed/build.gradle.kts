import com.install4j.gradle.Install4jTask
import de.undercouch.gradle.tasks.download.Download

plugins {
    id("triplea-java-library")
    id("application")
    alias(libs.plugins.shadow)
    alias(libs.plugins.install4j)
    alias(libs.plugins.download)
}

application {
    mainClass.set("org.triplea.game.client.HeadedGameRunner")
    applicationDefaultJvmArgs = listOf(
        //This flag fixes touch gestures in Java-21；Technical Reference: https://stackoverflow.com/questions/48535595/what-replaces-gestureutilities-in-java-9
        "--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED"
    )
}

val releasesDir = project.layout.buildDirectory.file("releases").get().asFile

fun getProductVersion(): String {
    return rootProject.file("game-app/run/.build/product-version.txt").readText().trim()
}

fun getCommitNumber(): String {
    return providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }.standardOutput.asText.get().trim()
}

val releaseVersion = getProductVersion() + "+" + getCommitNumber()

dependencies {
    implementation(project(":ai"))
    implementation(project(":domain-data"))
    implementation(project(":game-core"))
    implementation(project(":map-data"))
    implementation(project(":lobby-client"))
    implementation(project(":lobby-client-data"))
    implementation(project(":feign-common"))
    implementation(project(":java-extras"))
    implementation(project(":swing-lib"))
    implementation(project(":websocket-client"))
    testImplementation(project(":test-common"))
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("$group-$name")
    manifest {
        attributes("Main-Class" to application.mainClass.get())
    }
}

val assetsZipFileName = "game_headed_assets.zip"
val downloadAssets = tasks.register<Download>("downloadAssets") {
    src("https://github.com/triplea-game/assets/releases/download/47/$assetsZipFileName")
    dest(project.layout.buildDirectory.dir("downloads/assets-zip"))
    overwrite(false)
    onlyIfModified(true)
    quiet(true)
}

val unzipAssets = tasks.register<Copy>("unzipAssets") {
    from(zipTree(downloadAssets.map { it.outputs.files.singleFile }))
    into(project.layout.buildDirectory.dir("assets"))
}

tasks.named<ProcessResources>("processResources") {
    from(unzipAssets) {
        into("assets")
    }
}

val platformInstallers = tasks.register<Install4jTask>("platformInstallers") {
    group = "release"
    description = "creates installer files using install4j (eg: install.exe)"
    dependsOn(tasks.named("shadowJar"))

    projectFile = file("build.install4j")
    release = releaseVersion
    license = System.getenv("INSTALL4J_LICENSE_KEY")

    val releasesDirSnapshot = project.layout.buildDirectory.file("releases").get().asFile
    doLast {
        releasesDirSnapshot.listFiles { f -> f.name.endsWith(".sh") }
            ?.forEach { it.setExecutable(true, false) }
    }
}

val portableInstaller = tasks.register<Zip>("portableInstaller") {
    group = "release"

    from(file(".triplea-root"))
    from(unzipAssets) {
        into("assets")
    }
    from(file("dice_servers")) {
        into("dice_servers")
    }
    from(tasks.named("shadowJar")) {
        into("bin")
    }
}

tasks.register<Copy>("release") {
    group = "release"
    dependsOn(platformInstallers)

    from(portableInstaller)
    from(file("$releasesDir/TripleA_${releaseVersion}_macos.dmg"))
    from(file("$releasesDir/TripleA_${releaseVersion}_unix.sh"))
    from(file("$releasesDir/TripleA_${releaseVersion}_windows-64bit.exe"))
    into(file(project.layout.buildDirectory.dir("artifacts")))

    doFirst {
        inputs.files.forEach {
            if (!it.exists()) {
                throw GradleException("artifact '$it' does not exist")
            }
        }
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    dependsOn(unzipAssets)

    // "archiveVersion" sets the version number on packaged jar files
    // eg: "2.6+105234" in "lobby-server-2.6+50370c.jar"
    archiveVersion.set(releaseVersion)
    archiveClassifier.set("")
}
