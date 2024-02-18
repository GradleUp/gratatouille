plugins {
    `embedded-kotlin`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.plugin)
    implementation(libs.gradle.api)
    implementation(libs.vespene)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.buildkonfig)
    implementation(libs.ksp.gradle.plugin)
}

group = "build-logic"
