plugins {
    id("org.jetbrains.kotlin.jvm")
    id("gratatouille.gradle.plugin")
    id("java-gradle-plugin")
}

dependencies {
    implementation(gradleApi())
    gratatouille(project(":implementation"))
}

gradlePlugin {
    plugins {
        create("testplugin") {
            this.implementationClass = "testplugin.TestPlugin"
            this.id = "testplugin"
        }
    }
}