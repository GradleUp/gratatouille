plugins {
    id("org.jetbrains.kotlin.jvm")
}

module()

dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.ksp.api)
    implementation(libs.cast)
}

