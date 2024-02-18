pluginManagement {
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            mavenCentral()
            google()
        }
    }
}

includeBuild("../sample-plugin")