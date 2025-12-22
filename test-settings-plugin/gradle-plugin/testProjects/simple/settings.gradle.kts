pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.mavenCentral()
    it.maven(rootDir.resolve("../../../../build/m2"))
    it.maven("https://storage.googleapis.com/gradleup/m2")
  }
}

plugins {
  id("testSettingsPlugin1").version("0.0.0")
  id("testSettingsPlugin2").version("0.0.0")
}

testSettingsPlugin1 {
  foo = "foo"
}