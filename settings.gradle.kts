pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      google()
      gradlePluginPortal()
    }
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("libs.versions.toml"))
    }
  }
}

include(":gratatouille-core", ":gratatouille-gradle-plugin", ":gratatouille-processor")
includeBuild("build-logic")
