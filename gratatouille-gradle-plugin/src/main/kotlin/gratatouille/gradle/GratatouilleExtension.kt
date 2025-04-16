package gratatouille.gradle

import com.gradleup.gratatouille.gradle.BuildConfig
import gratatouille.gradle.tasks.registerGenerateDescriptorTask
import gratatouille.gradle.tasks.registerZipFilesTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

abstract class GratatouilleExtension(private val project: Project) {
  /**
   * Enables code generation
   */
  fun codeGeneration() {
    codeGeneration { }
  }

  /**
   * Enables code generation
   */
  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  fun codeGeneration(action: Action<CodeGenerationSpec>) {
    val codeGenerationSpec = CodeGenerationSpec(project)
    action.execute(codeGenerationSpec)

    require(project.pluginManager.hasPlugin("com.google.devtools.ksp")) {
      "Gratatouille: using code generation requires the 'com.google.devtools.ksp' plugin"
    }
    require(project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm")) {
      "Gratatouille: using code generation requires the 'org.jetbrains.kotlin.jvm' plugin"
    }

    project.configurations.getByName("ksp").dependencies.add(
      project.dependencies.create("${BuildConfig.group}:gratatouille-processor")
    )

    if (codeGenerationSpec.publishedCoordinates != null) {
      project.kspExtension.arg("implementationCoordinates", codeGenerationSpec.publishedCoordinates!!)
    }
    project.kspExtension.arg("enableKotlinxSerialization", codeGenerationSpec.enableKotlinxSerialization.orElse(false).get().toString())

    project.configurations.getByName("implementation").dependencies.add(
      project.dependencies.create("${BuildConfig.group}:gratatouille-runtime")
    )
  }

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
  fun pluginDescriptor(id: String, implementationClass: String) {
    val directory = registerDescriptorTask(id, implementationClass)
    // From https://github.com/gradle/gradle/blob/master/platforms/extensibility/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/JavaGradlePluginPlugin.java#L253
    project.tasks.named("processResources", Copy::class.java) {
      val copyPluginDescriptors: CopySpec = it.rootSpec.addChild()
      copyPluginDescriptors.into("/")
      copyPluginDescriptors.from(directory)
    }
  }

  /**
   * Creates a new `${pluginId}PluginMarkerMaven` publication allowing to locate the implementation coordinates from the plugin id.
   * This method requires that only a single publication is present in this project. See other overloads if you need more control over publications.
   *
   * @throws IllegalStateException if there are more
   */
  fun pluginMarker(id: String) {
    require(project.pluginManager.hasPlugin("maven-publish")) {
      "Gratatouille: calling createMarkerPublication() requires the `maven-publish` plugin"
    }
    project.extensions.getByType(PublishingExtension::class.java).apply {
      val publications = this.publications.toList()
      check(publications.isNotEmpty()) {
        "Gratatouille: the project does not contain any publications. Create a publication before calling 'pluginMarker()'."
      }
      check(publications.size == 1) {
        "Gratatouille: the project contains multiple publications (${publications.joinToString(", ") { it.name }}). Use 'pluginMarker(String, String)' to specify the publication to use."
      }
      val publication = publications.single()
      check (publication is MavenPublication) {
        "Gratatouille: the publication is not an instance of MavenPublication."
      }
      createMarkerPublication(id, publication)
    }
  }

  /**
   * Creates a new `${pluginId}PluginMarkerMaven` publication allowing to locate the implementation coordinates from the plugin id.
   *
   * @param mainPublication the publication to redirect to.
   */
  fun pluginMarker(id: String, mainPublication: String) {
    require(project.pluginManager.hasPlugin("maven-publish")) {
      "Gratatouille: calling createMarkerPublication() requires the `maven-publish` plugin"
    }
    project.extensions.getByType(PublishingExtension::class.java).apply {
      val publication = publications.findByName(mainPublication)
      check(publication != null) {
        "Gratatouille: cannot find publication named '$mainPublication'"
      }
      check(publication is MavenPublication) {
        "Gratatouille: publication '$mainPublication' is not an instance of MavenPublication"
      }
      createMarkerPublication(id, publication)
    }
  }

  private fun registerDescriptorTask(id: String, implementationClass: String): Provider<Directory> {
    val task = project.registerGenerateDescriptorTask(
      taskName = "generate${id.displayName()}Descriptor",
      id = project.provider { id },
      implementationClass = project.provider { implementationClass }
    )

    return task.flatMap { it.output }
  }

  private fun createMarkerPublication(id: String, mainPublication: MavenPublication) {
    require(project.pluginManager.hasPlugin("maven-publish")) {
      "Gratatouille: calling createMarkerPublication() requires the `maven-publish` plugin"
    }
    project.extensions.getByType(PublishingExtension::class.java).apply {
      // https://github.com/gradle/gradle/blob/master/platforms/extensibility/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/MavenPluginPublishPlugin.java#L88
      publications.create(id.displayName() + "PluginMarkerMaven", MavenPublication::class.java) { markerPublication ->
        markerPublication.trySetAlias()
        markerPublication.artifactId = "$id.gradle.plugin"
        markerPublication.groupId = id

        val groupProvider = project.provider { mainPublication.groupId }
        val artifactIdProvider = project.provider { mainPublication.artifactId }
        val versionProvider = project.provider { mainPublication.version }
        markerPublication.pom.withXml { xmlProvider ->
          val root = xmlProvider.asElement()
          val document = root.ownerDocument
          val dependencies = root.appendChild(document.createElement("dependencies"))
          val dependency = dependencies.appendChild(document.createElement("dependency"))
          val groupId = dependency.appendChild(document.createElement("groupId"))
          groupId.textContent = groupProvider.get()
          val artifactId = dependency.appendChild(document.createElement("artifactId"))
          artifactId.textContent = artifactIdProvider.get()
          val version = dependency.appendChild(document.createElement("version"))
          version.textContent = versionProvider.get()
        }
      }
    }
  }
}

class CodeGenerationSpec(private val project: Project) {
  internal var publishedCoordinates: String? = null

  val enableKotlinxSerialization = project.objects.property(Boolean::class.java)

  /**
   * Enables ClassLoader isolation mode using the project group, name and version as published coordinates
   */
  fun classLoaderIsolation() {
    classLoaderIsolation("${project.group}:${project.name}:${project.version}")
  }

  /**
   * Enables ClassLoader isolation.
   *
   * @param publishedCoordinates the coordinates of the implementation to use in the isolated classpath.
   */
  fun classLoaderIsolation(publishedCoordinates: String) {
    this.publishedCoordinates = publishedCoordinates

    // See https://github.com/google/ksp/issues/1677
    project.tasks.withType(AbstractArchiveTask::class.java).configureEach {
      it.exclude("META-INF/gratatouille/**")
    }

    // this should be safe because we require `org.jetbrains.kotlin.jvm`
    val adhocComponentWithVariants = project.components.getByName("java") as AdhocComponentWithVariants

    val configuration = project.configurations.create("gratatouilleApiElements") {
      it.isCanBeConsumed = true
      it.isCanBeResolved = false
      it.attributes {
        it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, USAGE_GRATATOUILLE))
      }
    }

    val exportedFiles = project.registerZipFilesTask(
      taskName = "gratatouilleZipPluginSources",
      // TODO: is there a way to not hardcode the destination here?
      inputFiles = project.fileTree("build/generated/ksp/main/resources/META-INF/gratatouille/")
    )
    exportedFiles.configure {
      // TODO: is there a way to wire this automagically
      it.dependsOn("kspKotlin")
    }

    project.artifacts.add(configuration.name, exportedFiles)

    adhocComponentWithVariants.addVariantsFromConfiguration(configuration) {}
  }
}

