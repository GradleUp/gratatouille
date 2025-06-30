plugins {
    alias(libs.plugins.kgp)
    alias(libs.plugins.compat.patrouille)
}

compatPatrouille {
    java(17)
    kotlin(embeddedKotlinVersion)
}
dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.gradle.api)
    implementation(libs.vespene)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.buildkonfig)
    implementation(libs.librarian)
    implementation(libs.nmcp)
    implementation(libs.gratatouille.gradle.plugin)
}

group = "gratatouille-build-logic"

