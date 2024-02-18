package gratatouille

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File

annotation class GInternal
annotation class GManuallyWired
annotation class GTaskAction(val name: String = "", val group: String = "", val description: String = "")

typealias GOutputFile = File
typealias GOutputDirectory = File

typealias GInputFile = File
typealias GInputDirectory = File
typealias GInputFiles = Set<File>

val json = Json {
  classDiscriminator = "#class"
}

internal inline fun <reified T> File.decodeJson(): T {
  return json.decodeFromString<T>(readText())
}

internal fun <T> File.decodeJson(clazz: Class<T>): T {
  @Suppress("UNCHECKED_CAST")
  return json.decodeFromString(serializer(clazz), readText()) as T
}

internal inline fun <reified T> T.encodeJson(): String {
  return json.encodeToString(this)
}

internal inline fun <reified T> T.encodeJsonTo(file: File) {
  return file.writeText(json.encodeToString(this))
}