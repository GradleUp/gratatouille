plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.ksp.api)
}

configureLib()