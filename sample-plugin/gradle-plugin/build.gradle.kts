plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.gradleup.gratatouille.plugin")
    id("java-gradle-plugin")
}

version = rootProject.version

dependencies {
    gratatouille(project(":implementation"))
}

gradlePlugin {
    plugins {
        create("recipes") {
            this.implementationClass = "recipes.RecipesPlugin"
            this.id = "recipes"
        }
    }
}