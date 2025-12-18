package gratatouille.gradle.internal

import gratatouille.gradle.tasks.registerGenerateDescriptorTask
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectPublicationRegistry
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.internal.Describables
import org.gradle.internal.DisplayName
import org.gradle.plugin.use.PluginId
import org.gradle.plugin.use.internal.DefaultPluginId
import org.gradle.plugin.use.resolve.internal.local.PluginPublication

internal fun Project.pluginDescriptor(id: String, implementationClass: String) {
  val directory = registerDescriptorTask(id, implementationClass)
  // From https://github.com/gradle/gradle/blob/master/platforms/extensibility/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/JavaGradlePluginPlugin.java#L253
  tasks.named("processResources", Copy::class.java) {
    val copyPluginDescriptors: CopySpec = it.rootSpec.addChild()
    copyPluginDescriptors.into("/")
    copyPluginDescriptors.from(directory)
  }
}

internal fun Project.pluginMarker(id: String) {
  require(pluginManager.hasPlugin("maven-publish")) {
    "Gratatouille: calling createMarkerPublication() requires the `maven-publish` plugin"
  }
  extensions.getByType(PublishingExtension::class.java).apply {
    val publications = this.publications.filter {
      // Filter out our own publications in case there are several plugin ids in the same jar
      !it.name.endsWith("PluginMarkerMaven")
    }.toList()
    check(publications.isNotEmpty()) {
      "Gratatouille: the project does not contain any publications. Create a publication before calling 'pluginMarker()'."
    }
    check(publications.size == 1) {
      "Gratatouille: the project contains multiple publications (${publications.joinToString(", ") { it.name }}). Use 'pluginMarker(String, String)' to specify the publication to use."
    }
    val publication = publications.single()
    check(publication is MavenPublication) {
      "Gratatouille: the publication is not an instance of MavenPublication."
    }
    createMarkerPublication(id, publication)
  }
}

internal fun Project.pluginMarker(id: String, mainPublication: String) {
  require(pluginManager.hasPlugin("maven-publish")) {
    "Gratatouille: calling createMarkerPublication() requires the `maven-publish` plugin"
  }
  extensions.getByType(PublishingExtension::class.java).apply {
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

private fun Project.registerDescriptorTask(id: String, implementationClass: String): Provider<Directory> {
  val task = registerGenerateDescriptorTask(
    taskName = "generate${id.displayName()}Descriptor",
    id = provider { id },
    implementationClass = provider { implementationClass }
  )

  return task.flatMap { it.output }
}

private fun Project.createMarkerPublication(id: String, mainPublication: MavenPublication) {
  require(pluginManager.hasPlugin("maven-publish")) {
    "Gratatouille: calling createMarkerPublication() requires the `maven-publish` plugin"
  }
  extensions.getByType(PublishingExtension::class.java).apply {
    // https://github.com/gradle/gradle/blob/master/platforms/extensibility/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/MavenPluginPublishPlugin.java#L88
    publications.create(id.displayName() + "PluginMarkerMaven", MavenPublication::class.java) { markerPublication ->
      markerPublication.trySetAlias()
      markerPublication.artifactId = "$id.gradle.plugin"
      markerPublication.groupId = id

      val groupProvider = provider { mainPublication.groupId }
      val artifactIdProvider = provider { mainPublication.artifactId }
      val versionProvider = provider { mainPublication.version }
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

private class LocalPluginPublication(private val name: String, private val id: String) : PluginPublication {
  override fun getDisplayName(): DisplayName {
    return Describables.withTypeAndName("plugin", name)
  }

  override fun getPluginId(): PluginId {
    return DefaultPluginId.of(id)
  }
}

internal fun Project.pluginLocalPublication(id: String) {
  val registry = project.serviceOf<ProjectPublicationRegistry>()

  val projectInternal = project as ProjectInternal
  val publication = LocalPluginPublication("Gratatouille generated local publication for $id", id)

  val registerPublication = ProjectPublicationRegistry::class.java.methods.single { it.name == "registerPublication" }
  if (registerPublication.parameters.first().type.name == "org.gradle.api.internal.project.ProjectIdentity") {
    // newer Gradle has the notion of project identity
    val identity = ProjectInternal::class.java.methods.single { it.name == "getProjectIdentity" }.invoke(projectInternal)
    registerPublication.invoke(registry, identity, publication)
  } else {
    // older Gradle just passes `ProjectInternal`
    registry.registerPublication(projectInternal, publication)
  }
}