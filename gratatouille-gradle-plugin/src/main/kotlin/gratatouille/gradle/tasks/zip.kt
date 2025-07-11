package gratatouille.gradle.tasks

import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@GTask
fun zipFiles(inputFiles: GInputFiles, outputFile: GOutputFile) {
  ZipOutputStream(outputFile.outputStream()).use { zipOutputStream ->
    inputFiles.forEach {
      if (it.file.isFile) {
        zipOutputStream.putNextEntry(ZipEntry(it.normalizedPath))
        it.file.inputStream().use {
          it.copyTo(zipOutputStream)
        }
      }
    }
  }
}