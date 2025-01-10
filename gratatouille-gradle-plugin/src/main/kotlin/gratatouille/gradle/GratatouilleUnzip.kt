package gratatouille.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.zip.ZipInputStream

abstract class GratatouilleUnzip: DefaultTask() {
  @get:InputFiles
  abstract val inputFiles: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun taskAction() {
    val output = outputDirectory.get().asFile
    output.deleteRecursively()
    inputFiles.files.forEach {
      ZipInputStream(it.inputStream()).use { zipInputStream ->
        var entry = zipInputStream.nextEntry
        while (entry != null) {
          if (!entry.isDirectory) {
            output.resolve(entry.name).apply {
              parentFile.mkdirs()
              outputStream().use { outputStream ->
                zipInputStream.copyTo(outputStream)
              }
            }
          }
          entry = zipInputStream.nextEntry
        }
      }
    }
  }
}