package gratatouille.gradle.internal

import gratatouille.gradle.GratatouilleExtension
import gratatouille.gradle.tasks.registerUnzipFilesTask
import gratatouille.wiring.GExtension
import org.gradle.api.Project
import org.gradle.api.attributes.Usage

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
