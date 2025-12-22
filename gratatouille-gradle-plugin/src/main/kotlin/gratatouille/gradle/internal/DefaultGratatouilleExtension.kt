package gratatouille.gradle.internal

import gratatouille.gradle.GratatouilleExtension
import gratatouille.gradle.tasks.registerUnzipFilesTask
import gratatouille.wiring.GExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.attributes.Usage
import org.gradle.util.GradleVersion
import tapmoc.TapmocExtension

@Suppress("unused")
@GExtension("com.gradleup.gratatouille", "gratatouille", GratatouilleExtension::class)
abstract class DefaultGratatouilleExtension(private val project: Project): GratatouilleExtension {
  init {
    addDependencies = true

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
    project.configureCodeGeneration(this)
    project.pluginManager.apply("com.gradleup.tapmoc")
  }

  private fun kotlinVersionFor(version: GradleVersion): String {

    // See https://docs.gradle.org/current/userguide/compatibility.html
    return when  {
      version >= GradleVersion.version("9.2.0") -> "2.2.0"
      version >= GradleVersion.version("9.0.0") -> "2.0.0"
      version >= GradleVersion.version("8.12") -> "2.0.21"
      version >= GradleVersion.version("8.11") -> "2.0.20"
      version >= GradleVersion.version("8.10") -> "1.9.24"
      version >= GradleVersion.version("8.9") -> "1.9.23"
      version >= GradleVersion.version("8.7") -> "1.9.22"
      version >= GradleVersion.version("8.5") -> "1.9.20"
      version >= GradleVersion.version("8.4") -> "1.9.10"
      version >= GradleVersion.version("8.3") -> "1.9.0"
      version >= GradleVersion.version("8.2") -> "1.8.20"
      version >= GradleVersion.version("8.0") -> "1.8.10"
      version >= GradleVersion.version("7.6") -> "1.7.10"
      version >= GradleVersion.version("7.5") -> "1.6.21"
      version >= GradleVersion.version("7.3") -> "1.5.31"
      version >= GradleVersion.version("7.2") -> "1.5.21"
      version >= GradleVersion.version("7.0") -> "1.4.31"
      else -> error("Unsupported Gradle version: $version")
    }
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
