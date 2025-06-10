pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      google()
      maven("https://storage.googleapis.com/gradleup/m2")
    }
  }
}

include(":gratatouille-wiring-runtime", ":gratatouille-tasks-runtime", ":gratatouille-gradle-plugin", ":gratatouille-processor")
includeBuild("gratatouille-build-logic")
