pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.mavenCentral()
    it.maven(rootDir.resolve("../../../../build/m2"))
  }
}
