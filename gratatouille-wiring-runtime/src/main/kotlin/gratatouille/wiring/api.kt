package gratatouille.wiring

import kotlin.reflect.KClass

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
annotation class GExtension(val pluginId: String, val extensionName: String = "", val publicType: KClass<*> = Any::class)

fun String.capitalizeFirstLetter(): String {
  return replaceFirstChar { if (it.isLowerCase()) it.titlecaseChar() else it }
}

fun String.decapitalizeFirstLetter(): String {
  return replaceFirstChar { if (it.isUpperCase()) it.lowercaseChar() else it }
}