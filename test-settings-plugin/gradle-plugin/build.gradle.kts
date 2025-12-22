plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.gradleup.gratatouille")
  id("com.google.devtools.ksp")
  id("maven-publish")
}

version = "0.0.0"

publishing {
  publications {
    create("default", MavenPublication::class.java) {
      from(components["java"])
    }
  }
  repositories {
    maven {
      name = "pluginTest"
      url = uri(rootDir.resolve("../build/m2"))
    }
  }
}

dependencies {
  compileOnly(libs.gradle.api)
  testImplementation(gradleTestKit())
  testImplementation(kotlin("test"))
}

gratatouille {
  pluginMarker("testSettingsPlugin1")
  pluginMarker("testSettingsPlugin2")
}
tasks.withType(Test::class.java) {
  dependsOn("publishAllPublicationsToPluginTestRepository")
  dependsOn(gradle.includedBuild("gratatouille").task(":publishAllPublicationsToPluginTestRepository"))
}