import com.gradleup.librarian.gradle.librarianModule

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.gmazzo.buildconfig")
    id("java-gradle-plugin")
}

librarianModule()

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
        create("com.gradleup.gratatouille.implementation") {
            this.implementationClass = "gratatouille.gradle.GratatouilleImplementationPlugin"
            this.id = "com.gradleup.gratatouille.implementation"
        }
        create("com.gradleup.gratatouille.api") {
            this.implementationClass = "gratatouille.gradle.GratatouilleApiPlugin"
            this.id = "com.gradleup.gratatouille.api"
        }
        create("com.gradleup.gratatouille") {
            this.implementationClass = "gratatouille.gradle.GratatouillePlugin"
            this.id = "com.gradleup.gratatouille"
        }
    }
}
