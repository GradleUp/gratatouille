package gratatouille.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ArchiveOperations
import javax.inject.Inject

abstract class GratatouillePlugin : Plugin<Project> {
  @Inject
  abstract fun getArchiveOperations(): ArchiveOperations

  override fun apply(target: Project) {
    target.extensions.create("gratatouille", GratatouilleExtension::class.java, target)

    val configuration = target.configurations.create("gratatouille") {
      it.isCanBeConsumed = false

      it.attributes {
        it.attribute(Usage.USAGE_ATTRIBUTE, target.objects.named(Usage::class.java, USAGE_GRATATOUILLE))
      }
    }

    val unzipSources = target.tasks.register("gratatouilleUnzipPluginSources", GratatouilleUnzip::class.java) {
      it.inputFiles.from(configuration)
      it.outputDirectory.set(target.layout.buildDirectory.dir("gratatouille/unzipped"))
    }

    target.kotlinExtension.sourceSets.getByName("main").kotlin.srcDir(unzipSources)

    target.configureDefaultVersionsResolutionStrategy()
  }
}
