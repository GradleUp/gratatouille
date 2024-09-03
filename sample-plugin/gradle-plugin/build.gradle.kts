plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
    id("com.gradleup.gratatouille.api")
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