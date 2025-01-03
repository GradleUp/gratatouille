package gratatouille.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy

class DescriptorConnection(val directory: Provider<Directory>)

class PluginSpec(private val id: String, private val project: Project) {
  /**
   * Registers a `generate${pluginId}Descriptor` task that generates a [plugin descriptor](https://docs.gradle.org/current/userguide/java_gradle_plugin.html#sec:gradle_plugin_dev_usage) for the plugin.
   * That plugin descriptor is copied into the `.jar` file during the `processResources` task.
   * This allows to load a plugin by id.
   *
   * @param implementationClass the fully qualified class name for the plugin implementation. Example: `com.example.ExamplePlugin`
   */
  fun implementationClass(implementationClass: String) {
    implementationClass(implementationClass) { connection ->
      // From https://github.com/gradle/gradle/blob/master/platforms/extensibility/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/JavaGradlePluginPlugin.java#L253
      project.tasks.named("processResources", Copy::class.java) {
        val copyPluginDescriptors: CopySpec = it.rootSpec.addChild()
        copyPluginDescriptors.into("/")
        copyPluginDescriptors.from(connection.directory)
      }
    }
  }

  /**
   * Registers a `generate${pluginId}Descriptor` task that generates a [plugin descriptor](https://docs.gradle.org/current/userguide/java_gradle_plugin.html#sec:gradle_plugin_dev_usage) for the plugin.
   * Use [DescriptorConnection] to wire the descriptor to other tasks
   *
   * @param implementationClass the fully qualified class name for the plugin implementation. Example: `com.example.ExamplePlugin`
   */
  fun implementationClass(implementationClass: String, action: Action<DescriptorConnection>) {
    val task = project.tasks.register("generate${id.displayName()}Descriptor", GenerateDescriptorTask::class.java) {
      it.id.set(id)
      it.implementationClass.set(implementationClass)

      it.output.set(project.layout.buildDirectory.dir("gratatouille/descriptor/$id"))
    }

    val connection = DescriptorConnection(task.flatMap { it.output })
    action.execute(connection)
  }

  /**
   * Creates a new `${pluginId}PluginMarkerMaven` publication allowing to locate the implementation from the plugin id.
   *
   * @param mainPublication the publication to redirect to.
   */
  fun mainPublication(mainPublication: MavenPublication) {
    project.withRequiredPlugins("maven-publish") {
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
  }

  fun mainPublication(mainPublication: String) {
    project.withRequiredPlugins("maven-publish") {
      project.extensions.getByType(PublishingExtension::class.java).apply {
        val publication = publications.findByName(mainPublication)
        check(publication != null) {
          "Gratatouille: cannot find publication named '$mainPublication'"
        }
        check(publication is MavenPublication) {
          "Gratatouille: publication '$mainPublication' is not an instance of MavenPublication"
        }
        mainPublication(publication)
      }
    }
  }
}

private fun String.displayName() =
  this.split(".").joinToString(separator = "") { it.replaceFirstChar { it.uppercase() } }

abstract class GratatouilleExtension(private val project: Project) {
  fun plugin(id: String, action: Action<PluginSpec>) {
    val spec = PluginSpec(id, project)
    action.execute(spec)
  }
}

