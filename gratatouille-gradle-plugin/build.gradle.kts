plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.gmazzo.buildconfig")
    id("com.google.devtools.ksp")
    id("com.gradleup.gratatouille")
}

module()

dependencies {
    compileOnly(libs.ksp.api)
    compileOnly(libs.gradle.api)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)

    testImplementation(libs.kotlin.test)
}

buildConfig {
    useKotlinOutput()
    packageName("com.gradleup.gratatouille.gradle")

    buildConfigField("group", group.toString())
}

gratatouille {
    codeGeneration()
}

// When used in an included build, use the remote coordinates
configurations.configureEach {
    resolutionStrategy {
        useGlobalDependencySubstitutionRules.set(false)
    }
}
