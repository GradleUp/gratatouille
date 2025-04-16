plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.gmazzo.buildconfig")
    id("java-gradle-plugin")
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

gradlePlugin {
    plugins {
        create("com.gradleup.gratatouille") {
            this.implementationClass = "gratatouille.gradle.GratatouillePlugin"
            this.id = "com.gradleup.gratatouille"
        }
    }
}
