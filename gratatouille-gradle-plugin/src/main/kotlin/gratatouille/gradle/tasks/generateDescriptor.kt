package gratatouille.gradle.tasks

import gratatouille.tasks.GOutputDirectory
import gratatouille.tasks.GTask
import java.util.*

@GTask
fun generateDescriptor(id: String, implementationClass: String, output: GOutputDirectory) {
  output.resolve("META-INF/gradle-plugins").apply {
    mkdirs()
    val properties = Properties().apply {
      this.put("implementation-class", implementationClass)
    }

    resolve("$id.properties").outputStream().use {
      properties.store(it, "Gradle plugin descriptor (auto-generated)")
    }
  }
}