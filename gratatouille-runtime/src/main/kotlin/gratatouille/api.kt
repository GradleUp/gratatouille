package gratatouille

import java.io.File

/**
 * Indicates that the given function is the implementation of a task:
 *
 * ```kotlin
 * @GTask(
 *   name = "cook",
 *   group = "com.recipe",
 *   description = "Prepares ingredients for the given number of guests"
 * )
 * internal fun prepareIngredients(persons: Int): Ingredients {
 *   ...
 * }
 * ```
 *
 * Given the above function, Gratatouille generates a `registerPrepareIngredientTask() {}` that registers and configures a task that
 * calls your function when needed:
 * ```
 * // Generated code.
 * internal fun Project.registerPrepareIngredientsTask(
 *   taskName: String = "prepareIngredients",
 *   taskDescription: String? = null,
 *   taskGroup: String? = null,
 *   persons: Provider<Int>,
 * ): TaskProvider<PrepareIngredientsTask> { ... }
 * ```
 *
 * Call `registerPrepareIngredientsTask()` in your plugin code.
 *
 * Internally, Gratatouille generates Gradle Tasks, Workers, entry points:
 *
 * ```kotlin
 * // Generated code
 * @CacheableTask
 * internal abstract class PrepareIngredientsTask : DefaultTask() {
 *   // creates PrepareIngredientsWorkAction
 * }
 *
 * private abstract class PrepareIngredientsWorkAction : WorkAction<PrepareIngredientsWorkParameters> {
 *   // calls PrepareIngredientsEntryPoint through reflection
 * }
 *
 * public class PrepareIngredientsEntryPoint {
 *   public companion object {
 *     @JvmStatic
 *     public fun run(persons: Int, outputFile: File) {
 *       // calls your function to do the actual work
 *       prepareIngredients(
 *         persons = persons,
 *       ).encodeJsonTo(outputFile)
 *     }
 *   }
 * }
 * ```
 *
 * @param name the name of the task. If empty, defaults to the name of the function.
 * @param group the group of the task. If empty, defaults to no group. Tasks without a group are not displayed in `./gradlew --tasks` by default.
 * @param description the description of the task. If empty, defaults to no description.
 * @param pure whether the annotated function is [pure](https://en.wikipedia.org/wiki/Pure_function), i.e. its outputs only depends
 * on the inputs. Impure functions are marked as non-cacheable and never [up-to-date](https://docs.gradle.org/current/userguide/incremental_build.html).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GTask(val name: String = "", val group: String = "", val description: String = "", val pure: Boolean = true)

/**
 * Generates a plugin class calling the target function and a plugin descriptor for that plugin.
 *
 * @param id the plugin id to use for this plugin
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GPlugin(val id: String)

/**
 * Generates a simple plugin class that registers an instance of the annotated class.
 *
 * The annotated class may have an `org.gradle.api.Project` constructor parameter.
 *
 * @param pluginId the plugin id to use for this plugin.
 * @param extensionName the name of the extension. By default, the name of the class with any 'Extension' suffix removes and decapitalized.
 * The generated plugin also uses [extensionName] with a 'Plugin' suffix.
 *
 * @see GPlugin
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GExtension(val pluginId: String, val extensionName: String = "")

/**
 * Indicates that the given parameter doesn't contribute snapshotting and should be marked `Internal`.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class GInternal

/**
 * Indicates that the given output parameter shouldn't be automatically set. Instead, the generated code has a required parameter to
 * specify the output location.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class GManuallyWired

/**
 * Customizes the name of an output file or directory. By default, the name of the parameter is used.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class GFileName(val name: String)

/**
 * An input file. The contents of the file as well as the relative path of the file are used for snapshotting.
 *
 * Changing the relative path of the file invalidates the task but copying the project all at once in a new location doesn't.
 * In Gradle terms, this uses `PathSensitive(PathSensitivity.RELATIVE)`.
 */
typealias GInputFile = File

/**
 * An input file with a normalized path
 */
class FileWithPath(val file: File, val normalizedPath: String)

/**
 * A collection of input files. The content of each file as well as the normalized path of each file are used for snapshotting.
 *
 * Changing the relative path of a file invalidates the task but copying the project all at once in a new location doesn't.
 * In Gradle terms, this uses `PathSensitive(PathSensitivity.RELATIVE)`.
 *
 * The input files are typically created with `fileTree()`
 */
typealias GInputFiles = List<FileWithPath>

@Deprecated(
  "Gratatouille has no input directories. Tasks work with files, not directories. Use GInputFiles instead, this ensures that the " +
      "filtering/normalization done inside the task is the same as the filtering/normalization done for snapshotting.",
  level = DeprecationLevel.ERROR
)
typealias GInputDirectory = Nothing
@Deprecated(
  "Gratatouille has no input directories. Tasks work with files, not directories. Use GInputFiles instead, this ensures that the " +
      "filtering/normalization done inside the task is the same as the filtering/normalization done for snapshotting.",
  level = DeprecationLevel.ERROR
)
typealias GInputDirectories = Nothing

/**
 * An output file. The content of the file is used for snapshotting.
 */
typealias GOutputFile = File
/**
 * An input directory. The content of each file and directory recursively is used for snapshotting.
 * Order shouldn't be relied on.
 */
typealias GOutputDirectory = File

/**
 * A typealias for a simple serializable type like simple types (String, Int, List, Map, etc...).
 *
 * For Gradle to be able to snapshot this properly, any value passed must implement `Serializable`.
 */
typealias GAny = Any

interface GLogger {
  fun debug(message: String)
  fun lifecycle(message: String)
  fun warn(message: String)
}