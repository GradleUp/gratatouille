package gratatouille.gradle.internal

import gratatouille.gradle.GratatouilleExtension
import gratatouille.gradle.tasks.registerUnzipFilesTask
import gratatouille.GExtension
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

    project.configureCodeGeneration(this)
    project.pluginManager.apply("com.gradleup.tapmoc")
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
