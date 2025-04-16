package gratatouille.gradle.tasks

import gratatouille.GInputFiles
import gratatouille.GOutputDirectory
import gratatouille.GTask
import java.util.zip.ZipInputStream

@GTask
fun unzipFiles(inputFiles: GInputFiles, outputDirectory: GOutputDirectory) {
  val output = outputDirectory
  output.deleteRecursively()
  inputFiles.forEach {
    ZipInputStream(it.file.inputStream()).use { zipInputStream ->
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
