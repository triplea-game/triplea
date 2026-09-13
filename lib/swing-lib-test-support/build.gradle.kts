plugins {
    id("triplea-java-library")
}

description = "TripleA library for utilities and components that depend only on core java, javax.swing, and java.awt"

dependencies {
    implementation(libs.assertj.core)
    implementation(libs.awaitility)
    implementation(libs.junit.jupiter.api)
    implementation(libs.junit.jupiter.params)
    implementation(libs.mockito.core)
    implementation(libs.mockito.junit.jupiter)
    implementation(project(":swing-lib"))
    implementation(project(":test-common"))
}
