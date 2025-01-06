package gratatouille.gradle

import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy

class PluginSpec(private val id: String, private val project: Project) {
  /**
   * Registers a `generate${pluginId}Descriptor` task that generates a [plugin descriptor](https://docs.gradle.org/current/userguide/java_gradle_plugin.html#sec:gradle_plugin_dev_usage) for the plugin
   * and wires it to the `processResources` task.
   *
   * The descriptor will be included in the .jar file making it possible to locate the plugin implementation from the plugin id.
   *
   * @param implementationClass the fully qualified class name for the plugin implementation. Example: `com.example.ExamplePlugin`.
   *
   * @see registerDescriptorTask
   */
  fun registerAndWireDescriptorTask(implementationClass: String) {
    val directory = registerDescriptorTask(implementationClass)
    // From https://github.com/gradle/gradle/blob/master/platforms/extensibility/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/JavaGradlePluginPlugin.java#L253
    project.tasks.named("processResources", Copy::class.java) {
      val copyPluginDescriptors: CopySpec = it.rootSpec.addChild()
      copyPluginDescriptors.into("/")
      copyPluginDescriptors.from(directory)
    }
  }

  /**
   * Registers a `generate${pluginId}Descriptor` task that generates a [plugin descriptor](https://docs.gradle.org/current/userguide/java_gradle_plugin.html#sec:gradle_plugin_dev_usage) for the plugin.
   *
   * @param implementationClass the fully qualified class name for the plugin implementation. Example: `com.example.ExamplePlugin`
   *
   * @return a [Provider] for the [Directory] that contains the descriptor. The [Directory] includes `META-INF/gradle-plugins` so that it can be included directly.
   */
  fun registerDescriptorTask(implementationClass: String): Provider<Directory> {
    val task = project.tasks.register("generate${id.displayName()}Descriptor", GenerateDescriptorTask::class.java) {
      it.id.set(id)
      it.implementationClass.set(implementationClass)

      it.output.set(project.layout.buildDirectory.dir("gratatouille/descriptor/$id"))
    }

    return task.flatMap { it.output }
  }

  /**
   * Creates a new `${pluginId}PluginMarkerMaven` publication allowing to locate the implementation coordinates from the plugin id.
   *
   * @param mainPublication the publication to redirect to.
   */
  fun createMarkerPublication(mainPublication: MavenPublication) {
    require(project.pluginManager.hasPlugin("maven-publish")) {
      "Gratatouille: calling createMarkerPublication() requires the `maven-publish` plugin"
    }
    project.extensions.getByType(PublishingExtension::class.java).apply {
      // https://github.com/gradle/gradle/blob/master/platforms/extensibility/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/MavenPluginPublishPlugin.java#L88
      publications.create(id.displayName() + "PluginMarkerMaven", MavenPublication::class.java) { markerPublication ->
        markerPublication.trySetAlias()
        markerPublication.artifactId = id
        markerPublication.artifactId = "$id.gradle.plugin";
        markerPublication.groupId = id;

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

  fun createMarkerPublication(mainPublication: String) {
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
      createMarkerPublication(publication)
    }
  }
}

