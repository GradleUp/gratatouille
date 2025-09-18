package gratatouille.gradle.internal

import com.gradleup.gratatouille.gradle.BuildConfig
import gratatouille.gradle.CodeGenerationSpec
import gratatouille.gradle.tasks.registerGenerateDescriptorTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy

internal enum class PluginVariant {
  Simple,
  Wiring,
  Tasks
}

internal fun Project.codeGeneration(action: Action<CodeGenerationSpec>, pluginVariant: PluginVariant) {
  val codeGenerationSpec = DefaultCodeGenerationSpec(this)
  action.execute(codeGenerationSpec)

  require(pluginManager.hasPlugin("com.google.devtools.ksp")) {
    "Gratatouille: using code generation requires the 'com.google.devtools.ksp' plugin"
  }
  require(pluginManager.hasPlugin("org.jetbrains.kotlin.jvm")) {
    "Gratatouille: using code generation requires the 'org.jetbrains.kotlin.jvm' plugin"
  }

  dependencies.add("ksp", dependencies.create("${BuildConfig.group}:gratatouille-processor"))

  if (codeGenerationSpec.addDependencies.getOrElse(true)) {
    val runtimes = when (pluginVariant) {
      PluginVariant.Simple -> listOf("wiring", "tasks")
      PluginVariant.Wiring -> listOf("wiring")
      PluginVariant.Tasks -> listOf("tasks")
    }
    runtimes.forEach {
      dependencies.add("implementation", dependencies.create("${BuildConfig.group}:gratatouille-$it-runtime"))
    }
  }

  val ciSpec = codeGenerationSpec.classloaderIsolationSpec
  if (ciSpec != null) {
    when (pluginVariant) {
      PluginVariant.Simple,
      PluginVariant.Wiring -> {
        error("To use classloader isolation, use the com.gradleup.gratatouille.tasks plugin")
      }

      PluginVariant.Tasks -> Unit
    }
    kspExtension.arg("implementationCoordinates", ciSpec.coordinates)
    kspExtension.arg("configurationName", ciSpec.configurationName)
  }
  kspExtension.arg("enableKotlinxSerialization", codeGenerationSpec.enableKotlinxSerialization.orElse(false).get().toString())
}


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