package gratatouille.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.org.apache.tools.zip.ZipEntry
import org.gradle.internal.impldep.org.apache.tools.zip.ZipOutputStream

abstract class GratatouilleZip: DefaultTask() {
  @get:InputFiles
  abstract val inputFiles: ConfigurableFileCollection

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    ZipOutputStream(outputFile.get().asFile.outputStream()).use { zipOutputStream ->
      inputFiles.asFileTree.visit {
        if (it.file.isFile) {
          zipOutputStream.putNextEntry(ZipEntry(it.path))
          it.file.inputStream().copyTo(zipOutputStream)
        }
      }
    }
  }
}