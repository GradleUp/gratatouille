plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.gradleup.gratatouille")
    id("java-gradle-plugin")
}

gradlePlugin {
    plugins {
        create("testplugin") {
            this.implementationClass = "testplugin.TestPlugin"
            this.id = "testplugin"
        }
    }
}