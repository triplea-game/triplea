plugins {
    id("java-library")
    id("eclipse")
    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.spotless).apply(false)
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.ALL
}

subprojects {
    apply(plugin = "java-library")

    dependencies {
        implementation(rootProject.libs.jlayer) {
            exclude(group = "junit", module = "junit")
        }
        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)

        testCompileOnly(rootProject.libs.lombok)
        testAnnotationProcessor(rootProject.libs.lombok)

        implementation(rootProject.libs.logback.classic)
        implementation(rootProject.libs.caffeine)
        implementation(rootProject.libs.gson)
        implementation(rootProject.libs.guava)
        implementation(rootProject.libs.dropwizard.websockets)
        implementation(rootProject.libs.jakarta.mail)
        implementation(rootProject.libs.jaxb.impl)
        implementation(rootProject.libs.commons.io)
        implementation(rootProject.libs.feign.gson)
        implementation(rootProject.libs.commons.math3)
        implementation(rootProject.libs.commons.text)
        implementation(rootProject.libs.apache.httpmime)
        implementation(rootProject.libs.java.websocket)
        implementation(rootProject.libs.jetbrains.annotations)
        implementation(rootProject.libs.xchart)
        implementation(rootProject.libs.radiance.substance)
        implementation(rootProject.libs.snakeyaml.engine)
        testImplementation(rootProject.libs.jackson.datatype.jsr310)
        testImplementation(rootProject.libs.hamcrest.optional)
        testImplementation(rootProject.libs.wiremock)
        testImplementation(rootProject.libs.equals.verifier)
        testImplementation(rootProject.libs.assertj.core)
        testImplementation(rootProject.libs.awaitility)
        testImplementation(rootProject.libs.hamcrest)
        testImplementation(rootProject.libs.bundles.junit)
        testImplementation(rootProject.libs.bundles.mockito)
        testImplementation(rootProject.libs.sonatype.goodies.prefs)
        testImplementation(rootProject.libs.bundles.xmlunit)
        testImplementation(rootProject.libs.wiremock.junit5)
        testRuntimeOnly(rootProject.libs.junit.jupiter.engine)
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
    }
}
