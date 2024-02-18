plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.gmazzo.buildconfig")
    id("java-gradle-plugin")
}

configureLib()

dependencies {
    implementation(libs.kotlin.test)
    compileOnly(gradleApi())
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

buildConfig {
    useKotlinOutput()
    packageName("com.gradleup.gratatouille.gradle")

    buildConfigField("version", version.toString())
    buildConfigField("group", group.toString())
}

gradlePlugin {
    plugins {
        create("gratatouille.implementation") {
            this.implementationClass = "gratatouille.gradle.GratatouilleImplementationPlugin"
            this.id = "gratatouille.implementation"
        }
        create("gratatouille.gradle.plugin") {
            this.implementationClass = "gratatouille.gradle.GratatouilleGradlePluginPlugin"
            this.id = "gratatouille.gradle.plugin"
        }
    }
}