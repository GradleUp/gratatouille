pluginManagement {
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            mavenCentral()
            google()
            gradlePluginPortal()
        }
    }
}

includeBuild("..")
includeBuild("../build-logic")
include(":implementation", ":gradle-plugin")