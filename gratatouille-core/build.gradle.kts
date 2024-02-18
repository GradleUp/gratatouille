plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

configureLib()

dependencies {
    implementation(libs.kotlin.test)
    api(libs.kotlinx.serialization.json)
}

