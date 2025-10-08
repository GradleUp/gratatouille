package gratatouille.gradle

import gratatouille.wiring.GPlugin
import gratatouille.gradle.internal.*
import gratatouille.gradle.tasks.registerUnzipFilesTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectPublicationRegistry
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.internal.Describables
import org.gradle.internal.DisplayName
import org.gradle.plugin.use.PluginId
import org.gradle.plugin.use.internal.DefaultPluginId
import org.gradle.plugin.use.resolve.internal.local.PluginPublication

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

  /**
   * Registers a local plugin publication so that the plugin is discoverable from included builds.
   *
   * This function uses Gradle internal APIs.
   *
   * @param id the plugin id
   */
  fun pluginLocalPublication(id: String)
}

interface GratatouilleExtension: CodeGeneratorExtension, WiringExtension

@GPlugin("com.gradleup.gratatouille")
fun plugin(project: Project) {
  project.extensions.create(GratatouilleExtension::class.java, "gratatouille", DefaultGratatouilleExtension::class.java, project)
}

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

  override fun pluginLocalPublication(id: String) {
    project.pluginLocalPublication(id)
  }
}

interface GratatouilleWiringExtension: CodeGeneratorExtension, WiringExtension

@GPlugin("com.gradleup.gratatouille.wiring")
fun wiringPlugin(project: Project) {
  project.extensions.create(GratatouilleWiringExtension::class.java, "gratatouille", DefaultGratatouilleWiringExtension::class.java, project)
}

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

  override fun pluginLocalPublication(id: String) {
    project.pluginLocalPublication(id)
  }
}

interface GratatouilleTasksExtension: CodeGeneratorExtension

@GPlugin("com.gradleup.gratatouille.tasks")
fun tasksPlugin(project: Project) {
  project.extensions.create(GratatouilleTasksExtension::class.java, "gratatouille", DefaultGratatouilleTasksExtension::class.java, project)
}

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



