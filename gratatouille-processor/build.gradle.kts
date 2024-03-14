plugins {
    id("org.jetbrains.kotlin.jvm")
}

configureLib {
    create("default", MavenPublication::class.java) {
        from(components["java"])
    }
}

dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.ksp.api)
}

