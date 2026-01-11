plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

module()

dependencies {
    api(libs.kotlinx.serialization.json)
}
