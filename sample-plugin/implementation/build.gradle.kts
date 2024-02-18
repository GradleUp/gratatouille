plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("gratatouille.implementation")
}

dependencies {
    implementation("com.gradleup.gratatouille:gratatouille-core")
    implementation(libs.kotlinx.serialization.json)
}
