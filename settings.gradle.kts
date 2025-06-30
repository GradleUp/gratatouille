pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      google()
    }
  }
  repositories {
    maven("https://storage.googleapis.com/gradleup/m2") {
      content {
        includeGroup("com.gradleup.librarian")
        includeGroup("com.gradleup.nmcp")
        includeGroup("com.gradleup.gratatouille")
      }
    }
  }
}

include(":gratatouille-wiring-runtime", ":gratatouille-tasks-runtime", ":gratatouille-gradle-plugin", ":gratatouille-processor")
includeBuild("gratatouille-build-logic")
