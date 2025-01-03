package gratatouille.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.*

@CacheableTask
internal abstract class GenerateDescriptorTask: DefaultTask() {
  @get:Input
  abstract val id: Property<String>

  @get:Input
  abstract val implementationClass: Property<String>

  @get:OutputDirectory
  abstract val output: DirectoryProperty

  @TaskAction
  fun taskAction() {
    output.get().asFile.resolve("META-INF/gradle-plugins").apply {
      mkdirs()
      val properties = Properties().apply {
        this.put("implementation-class", implementationClass.get())
      }

      resolve(id.get() + ".properties").outputStream().use {
        properties.store(it, "Gradle plugin descriptor (auto-generated)")
      }
    }
  }
}
