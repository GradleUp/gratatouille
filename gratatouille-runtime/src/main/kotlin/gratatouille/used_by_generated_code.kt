package gratatouille

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File
import java.util.function.BiConsumer

@RequiresOptIn(message = "This symbol is only to be used by Gratatouille generated code")
annotation class GratatouilleInternal

@PublishedApi
internal val json = Json {
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

@GratatouilleInternal
class DefaultGLogger(private val callback: BiConsumer<Int, String>): GLogger {
  override fun debug(message: String) {
    callback.accept(0 /* LogLevel.DEBUG.ordinal */, message)
  }

  override fun lifecycle(message: String) {
    callback.accept(2 /* LogLevel.LIFECYCLE.ordinal */, message)
  }

  override fun warn(message: String) {
    callback.accept(3 /* LogLevel.WARN.ordinal */, message)
  }
}