package gratatouille

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File

@RequiresOptIn(message = "This symbol is only to be used by Gratatouille generated code")
annotation class GratatouilleInternal

annotation class GInternal
annotation class GManuallyWired
annotation class GTaskAction(val name: String = "", val group: String = "", val description: String = "")

typealias GOutputFile = File
typealias GOutputDirectory = File

typealias GInputFile = File
typealias GInputDirectory = File
typealias GInputFiles = List<FileWithPath>
class FileWithPath(val file: File, val normalizedPath: String)

val json = Json {
  classDiscriminator = "#class"
}

@GratatouilleInternal
inline fun <reified T> File.decodeJson(): T {
  return json.decodeFromString<T>(readText())
}

@GratatouilleInternal
fun <T> File.decodeJson(clazz: Class<T>): T {
  @Suppress("UNCHECKED_CAST")
  return json.decodeFromString(serializer(clazz), readText()) as T
}

@GratatouilleInternal
inline fun <reified T> T.encodeJson(): String {
  return json.encodeToString(this)
}

@GratatouilleInternal
inline fun <reified T> T.encodeJsonTo(file: File) {
  return file.writeText(json.encodeToString(this))
}

@GratatouilleInternal
fun List<Any>.toGInputFiles(): GInputFiles {
  return chunked(2).map {
    FileWithPath(it.get(0) as File, it.get(1) as String)
  }
}
