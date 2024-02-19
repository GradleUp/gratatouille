plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.gradleup.gratatouille.plugin")
    id("java-gradle-plugin")
}

dependencies {
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