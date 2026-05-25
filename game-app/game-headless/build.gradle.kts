import org.apache.tools.ant.filters.FixCrLfFilter
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("triplea-java-library")
    id("application")
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("org.triplea.game.server.HeadlessGameRunner")
}

dependencies {
    implementation(project(":lobby-client"))
    implementation(project(":lobby-client-data"))
    implementation(project(":ai"))
    implementation(project(":domain-data"))
    implementation(project(":game-core"))
    implementation(project(":java-extras"))
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("$group-$name")
    manifest {
        attributes("Main-Class" to application.mainClass.get())
    }
}

tasks.named<JavaExec>("run") {
    systemProperty("triplea.lobby.game.comments", "automated_host")
    systemProperty("triplea.lobby.uri", "http://127.0.0.1:3000")
    systemProperty("triplea.name", "Bot_Local01")
    systemProperty("triplea.port", "3300")
    systemProperty("triplea.server", "true")
}

val portableInstaller = tasks.register<Zip>("portableInstaller") {
    group = "release"

    from(file(".triplea-root"))
    from(file("scripts/run_bot"))
    from(file("scripts/run_bot.bat")) {
        filter(ReplaceTokens::class, "tokens" to mapOf("version" to project.version))
        filter(FixCrLfFilter::class, "eol" to FixCrLfFilter.CrLf.newInstance("crlf")) // workaround for https://github.com/gradle/gradle/issues/1151
    }
    from(tasks.named("shadowJar")) {
        into("bin")
    }
}

tasks.register<Copy>("release") {
    group = "release"

    from(portableInstaller)
    into(file(project.layout.buildDirectory.dir("artifacts")))

    doFirst {
        inputs.files.forEach {
            if (!it.exists()) {
                throw GradleException("artifact '$it' does not exist")
            }
        }
    }
}

tasks.register<Copy>("copyShadow") {
    from(tasks.named("shadowJar"))
    into("../infrastructure/ansible/roles/bot/files/")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}
