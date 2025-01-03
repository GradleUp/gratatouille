import com.gradleup.librarian.gradle.configureJavaCompatibility

plugins {
    `embedded-kotlin`
    alias(libs.plugins.librarian).apply(false)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.plugin)
    implementation(libs.gradle.api)
    implementation(libs.vespene)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.buildkonfig)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.librarian)
}

group = "build-logic"

/**
 * Ideally would use Runtime.version().feature() but the current Gradle still ships with Kotlin 1.9
 * that doesn't know about Java 22
 */
configureJavaCompatibility(17)
