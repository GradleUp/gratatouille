plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

configureLib {
    create("default", MavenPublication::class.java) {
        from(components["java"])
    }
}

dependencies {
    implementation(libs.kotlin.test)
    api(libs.kotlinx.serialization.json)
}
