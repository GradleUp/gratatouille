package gratatouille.gradle

import gratatouille.GPlugin
import gratatouille.gradle.tasks.registerUnzipFilesTask
import org.gradle.api.Project
import org.gradle.api.attributes.Usage

@GPlugin(id = "com.gradleup.gratatouille")
internal fun gratatouillePlugin(target: Project) {
  target.extensions.create("gratatouille", GratatouilleExtension::class.java, target)

  val configuration = target.configurations.create("gratatouille") {
    it.isCanBeConsumed = false

    it.attributes {
      it.attribute(Usage.USAGE_ATTRIBUTE, target.objects.named(Usage::class.java, USAGE_GRATATOUILLE))
    }
  }

  val unzipSources = target.registerUnzipFilesTask(
    taskName = "gratatouilleUnzipPluginSources",
    inputFiles = configuration,
  )

  target.kotlinExtension.sourceSets.getByName("main").kotlin.srcDir(unzipSources)

  target.configureDefaultVersionsResolutionStrategy()
}
