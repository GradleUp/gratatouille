plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.gradleup.gratatouille.api")
    id("java-gradle-plugin")
}

dependencies {
    gratatouille(project(":implementation"))
}

gradlePlugin {
    plugins {
        create("testplugin.isolated") {
            this.implementationClass = "testplugin.isolated.TestPluginIsolated"
            this.id = "testplugin.isolated"
        }
    }
}