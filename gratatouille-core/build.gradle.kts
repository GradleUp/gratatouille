import com.gradleup.librarian.gradle.librarianModule

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

librarianModule()

dependencies {
    implementation(libs.kotlin.test)
    api(libs.kotlinx.serialization.json)
}
