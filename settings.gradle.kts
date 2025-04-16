pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      google()
      maven("https://storage.googleapis.com/gradleup/m2")
    }
  }
}

include(":gratatouille-runtime", ":gratatouille-gradle-plugin", ":gratatouille-processor")
includeBuild("build-logic")
