package gratatouille.gradle

import gratatouille.GExtension
import gratatouille.gradle.internal.PluginVariant
import gratatouille.gradle.internal.USAGE_GRATATOUILLE
import gratatouille.gradle.internal.codeGeneration
import gratatouille.gradle.internal.configureDefaultVersionsResolutionStrategy
import gratatouille.gradle.internal.kotlinExtension
import gratatouille.gradle.internal.pluginDescriptor
import gratatouille.gradle.internal.pluginMarker
import gratatouille.gradle.tasks.registerUnzipFilesTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.attributes.Usage

interface CodeGeneratorExtension {
  /**
   * Enables code generation
   */
  fun codeGeneration()

  /**
   * Enables code generation
   */
  fun codeGeneration(action: Action<CodeGenerationSpec>)
}

interface WiringExtension {
  /**
   * Registers a `generate${pluginId}Descriptor` task that generates a [plugin descriptor](https://docs.gradle.org/current/userguide/java_gradle_plugin.html#sec:gradle_plugin_dev_usage) for the plugin
   * and wires it to the `processResources` task.
   *
   * The descriptor will be included in the .jar file making it possible to locate the plugin implementation from the plugin id.
   *
   * Calling this function is usually not needed if using code generation as code generation can use `@GPlugin` to get the plugin id.
   *
   * @param implementationClass the fully qualified class name for the plugin implementation. Example: `com.example.ExamplePlugin`.
   */
  fun pluginDescriptor(id: String, implementationClass: String)

  /**
   * Creates a new `${pluginId}PluginMarkerMaven` publication allowing to locate the implementation coordinates from the plugin id.
   * This method requires that only a single publication is present in this project. See other overloads if you need more control over publications.
   *
   * @throws IllegalStateException if there are more
   */
  fun pluginMarker(id: String)

  /**
   * Creates a new `${pluginId}PluginMarkerMaven` publication allowing to locate the implementation coordinates from the plugin id.
   *
   * @param mainPublication the publication to redirect to.
   */
  fun pluginMarker(id: String, mainPublication: String)
}

interface GratatouilleExtension: CodeGeneratorExtension, WiringExtension

@GExtension("com.gradleup.gratatouille", "gratatouille",)
abstract class DefaultGratatouilleExtension(private val project: Project): GratatouilleExtension {
  init {
    project.configureDefaultVersionsResolutionStrategy()
  }

  override fun codeGeneration() {
    project.codeGeneration(action = {}, pluginVariant = PluginVariant.Simple)
  }

  override fun codeGeneration(action: Action<CodeGenerationSpec>) {
    project.codeGeneration(action = action, pluginVariant = PluginVariant.Simple)
  }

  override fun pluginDescriptor(id: String, implementationClass: String) {
    project.pluginDescriptor(id, implementationClass)
  }

  override fun pluginMarker(id: String) {
    project.pluginMarker(id)
  }

  override fun pluginMarker(id: String, mainPublication: String) {
    project.pluginMarker(id, mainPublication)
  }
}

interface GratatouilleWiringExtension: CodeGeneratorExtension, WiringExtension

@GExtension("com.gradleup.gratatouille.wiring", "gratatouille1",)
abstract class DefaultGratatouilleWiringExtension(private val project: Project): GratatouilleWiringExtension {

  init {
    val configuration = project.configurations.create("gratatouille") {
      it.isCanBeConsumed = false

      it.attributes {
        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_GRATATOUILLE))
      }
    }

    val unzipSources = project.registerUnzipFilesTask(
      taskName = "gratatouilleUnzipPluginSources",
      inputFiles = configuration,
    )

    project.kotlinExtension.sourceSets.getByName("main").kotlin.srcDir(unzipSources)

    project.configureDefaultVersionsResolutionStrategy()
  }

  override fun codeGeneration() {
    project.codeGeneration(action = {}, pluginVariant = PluginVariant.Wiring)
  }

  override fun codeGeneration(action: Action<CodeGenerationSpec>) {
    project.codeGeneration(action = action, pluginVariant = PluginVariant.Wiring)
  }

  override fun pluginDescriptor(id: String, implementationClass: String) {
    project.pluginDescriptor(id, implementationClass)
  }

  override fun pluginMarker(id: String) {
    project.pluginMarker(id)
  }

  override fun pluginMarker(id: String, mainPublication: String) {
    project.pluginMarker(id, mainPublication)
  }
}

interface GratatouilleTasksExtension: CodeGeneratorExtension

@GExtension("com.gradleup.gratatouille.tasks", "gratatouille2",)
abstract class DefaultGratatouilleTasksExtension(private val project: Project): GratatouilleTasksExtension {
  init {
    project.configureDefaultVersionsResolutionStrategy()
  }

  override fun codeGeneration() {
    project.codeGeneration(action = {}, pluginVariant = PluginVariant.Tasks)
  }

  override fun codeGeneration(action: Action<CodeGenerationSpec>) {
    project.codeGeneration(action = action, pluginVariant = PluginVariant.Tasks)
  }
}



