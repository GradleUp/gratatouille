plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

module()

dependencies {
    implementation(libs.kotlin.test)
    compileOnly(libs.gradle.api)
}
