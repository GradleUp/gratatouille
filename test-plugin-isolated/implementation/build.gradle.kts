plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.gradleup.gratatouille.implementation")
    id("maven-publish")
}

version = "0.0.0"

publishing {
    publications {
        create("default", MavenPublication::class.java) {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "pluginTest"
            url = uri(rootDir.resolve("../build/m2"))
        }
    }
}

dependencies {
    implementation(libs.okio)
}