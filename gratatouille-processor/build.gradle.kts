import com.gradleup.librarian.gradle.librarianModule

plugins {
    id("org.jetbrains.kotlin.jvm")
}

librarianModule()

dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.ksp.api)
    implementation(libs.cast)
}

