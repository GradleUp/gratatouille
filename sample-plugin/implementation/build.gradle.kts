plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.gradleup.gratatouille.implementation")
}

version = rootProject.version

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
