pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.mavenCentral()
    it.maven(rootDir.resolve("../../../../build/m2"))
    it.maven("https://storage.googleapis.com/gradleup/m2")
  }
}
