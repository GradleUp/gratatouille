package gratatouille.wiring

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.file.FileCollection
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@RequiresOptIn(message = "This symbol is only to be used by Gratatouille generated wiring code")
annotation class GratatouilleWiringInternal

@PublishedApi
internal val json = Json {
  classDiscriminator = "#class"
}

@GratatouilleWiringInternal
inline fun <reified T> T.encodeJson(): String {
  return json.encodeToString(this)
}

@GratatouilleWiringInternal
inline fun <reified T> T.encodeJsonTo(file: File) {
  return file.writeText(json.encodeToString(this))
}

@GratatouilleWiringInternal
fun FileCollection.isolateFileCollection(): List<Any> = buildList {
  asFileTree.visit {
    /**
     * We used to do this using `FileCollection.filter {}` but looks like it's not working as expected.
     * See https://github.com/gradle/gradle/issues/34778
     */
    if (it.file.isFile) {
      add(it.file)
      add(it.path)
    }
  }
}

@GratatouilleWiringInternal
fun <T> T.isolate(): T {
  @Suppress("UNCHECKED_CAST")
  return when (this) {
    is Set<*> -> {
      this.map { it.isolate() }.toSet() as T
    }

    is List<*> -> {
      this.map { it.isolate() } as T
    }

    is Map<*, *> -> {
      entries.map { it.key.isolate() to it.value.isolate() }.toMap() as T
    }

    else -> this
  }
}

@GratatouilleWiringInternal
fun Method.invokeOrUnwrap(obj: Any?, vararg args: Any?): Any? {
  try {
    return invoke(obj, *args)
  } catch (e: InvocationTargetException) {
    /**
     * Unwrap the exception so it's displayed in Gradle error messages
     */
    throw e.cause!!
  }
}