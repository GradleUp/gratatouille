package gratatouille.gradle.internal

import gratatouille.gradle.CodeGenerationSpec
import gratatouille.gradle.tasks.registerZipFilesTask
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.tasks.bundling.AbstractArchiveTask

class DefaultCodeGenerationSpec(private val project: Project): CodeGenerationSpec {
  internal var publishedCoordinates: String? = null

  override val enableKotlinxSerialization = project.objects.property(Boolean::class.java)

  override val addDependencies = project.objects.property(Boolean::class.java)

  /**
   * Enables ClassLoader isolation mode using the project group, name and version as published coordinates
   */
  override fun classLoaderIsolation() {
    classLoaderIsolation("${project.group}:${project.name}:${project.version}")
  }

  /**
   * Enables ClassLoader isolation.
   *
   * @param publishedCoordinates the coordinates of the implementation to use in the isolated classpath.
   */
  override fun classLoaderIsolation(publishedCoordinates: String) {
    this.publishedCoordinates = publishedCoordinates

    // See https://github.com/google/ksp/issues/1677
    project.tasks.withType(AbstractArchiveTask::class.java).configureEach {
      it.exclude("META-INF/gratatouille/**")
    }

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
  }
}
