pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      google()
      gradlePluginPortal()
    }
  }
}

include(":gratatouille-core", ":gratatouille-gradle-plugin", ":gratatouille-processor")
includeBuild("build-logic")
