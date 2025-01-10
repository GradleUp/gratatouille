plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.gmazzo.buildconfig")
    id("java-gradle-plugin")
}

module()

dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.gratatouille.runtime)
    implementation(libs.ksp.api)
    implementation(project(":gratatouille-processor"))
    compileOnly(gradleApi())
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
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
