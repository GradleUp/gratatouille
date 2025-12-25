package gratatouille.gradle.internal

import com.gradleup.gratatouille.gradle.BuildConfig
import gratatouille.capitalizeFirstLetter
import gratatouille.gradle.CodeGenerationExtension
import gratatouille.gradle.GratatouilleTasksExtension
import gratatouille.gradle.VERSION
import gratatouille.gradle.tasks.registerZipFilesTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.tasks.bundling.AbstractArchiveTask

internal fun Project.configureCodeGeneration(
  extension: CodeGenerationExtension,
) {
  pluginManager.withPlugin("com.google.devtools.ksp") {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
      dependencies.add("ksp", dependencies.create("${BuildConfig.group}:gratatouille-processor:$VERSION"))
      kspExtension.arg("enableKotlinxSerialization", extension.enableKotlinxSerialization.orElse(false).map { it.toString() })

      val taskExtension = extension as? GratatouilleTasksExtension

      val implementation = configurations.named("implementation")
      implementation.configure { scope ->
        scope.withDependencies { dependencySet ->
          if (extension.addDependencies) {
            if (taskExtension == null) {
              dependencySet.addLater(provider {
                dependencies.create("${BuildConfig.group}:gratatouille-runtime:$VERSION")
              })
            }
            dependencySet.addLater(provider {
              dependencies.create("${BuildConfig.group}:gratatouille-tasks-runtime:$VERSION")
            })
          }
        }
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
