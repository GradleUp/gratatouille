package gratatouille.gradle.internal

import com.gradleup.gratatouille.gradle.BuildConfig
import gratatouille.gradle.ClassloaderIsolationSpec
import gratatouille.gradle.CodeGenerationExtension
import gratatouille.gradle.GratatouilleTasksExtension
import gratatouille.gradle.tasks.registerZipFilesTask
import gratatouille.wiring.capitalizeFirstLetter
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.configurationcache.problems.PropertyTrace

internal fun Project.configureCodeGeneration(
  extension: CodeGenerationExtension,
) {
  pluginManager.withPlugin("com.google.devtools.ksp") {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
      dependencies.add("ksp", dependencies.create("${BuildConfig.group}:gratatouille-processor"))
      kspExtension.arg("enableKotlinxSerialization", extension.enableKotlinxSerialization.orElse(false).map { it.toString() })

      val taskExtension = extension as? GratatouilleTasksExtension
      if (extension.addDependencies) {
        if (taskExtension == null) {
          // This is the main plugin
          dependencies.add("implementation", dependencies.create("${BuildConfig.group}:gratatouille-runtime"))
        }
        dependencies.add("implementation", dependencies.create("${BuildConfig.group}:gratatouille-tasks-runtime"))
      }

      if (taskExtension != null) {
        kspExtension.arg("implementationCoordinates", taskExtension.coordinates)
        kspExtension.arg("configurationName", taskExtension.configurationName)

        classLoaderIsolation()
      }
    }
  }
}


internal fun String.sanitize(): String {
  return this.split(Regex("[^a-zA-Z0-9]")).joinToString(separator = "") {
    it.capitalizeFirstLetter()
  }
}

/**
 * Enables ClassLoader isolation mode using the project group, name, and version as published coordinates
 */
private fun Project.classLoaderIsolation() {
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

  project.artifacts.add(configuration.name, exportedFiles) {
    it.classifier = "gratatouille"
    it.extension = "zip"
  }

  adhocComponentWithVariants.addVariantsFromConfiguration(configuration) {}

  // See https://github.com/google/ksp/issues/1677
  project.tasks.withType(AbstractArchiveTask::class.java).configureEach {
    it.exclude("META-INF/gratatouille/**")
  }
}
