package gratatouille.gradle.internal

import gratatouille.gradle.ClassloaderIsolationSpec
import gratatouille.gradle.CodeGenerationSpec
import gratatouille.gradle.tasks.registerZipFilesTask
import gratatouille.wiring.capitalizeFirstLetter
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.tasks.bundling.AbstractArchiveTask

class DefaultCodeGenerationSpec(private val project: Project): CodeGenerationSpec {
  internal var classloaderIsolationSpec: ClassloaderIsolationSpec? = null

  override val enableKotlinxSerialization = project.objects.property(Boolean::class.java)

  override val addDependencies = project.objects.property(Boolean::class.java)

  override fun classLoaderIsolation() {
    classLoaderIsolation {  }
  }

  private fun String.sanitize(): String {
    return this.split(Regex("[^a-zA-Z]")).joinToString(separator = "") {
      it.capitalizeFirstLetter()
    }
  }

  /**
   * Enables ClassLoader isolation mode using the project group, name and version as published coordinates
   */
  override fun classLoaderIsolation(action: Action<ClassloaderIsolationSpec>) {
    if (classloaderIsolationSpec == null) {
      classloaderIsolationSpec = project.objects.newInstance(ClassloaderIsolationSpec::class.java)
      val defaultCoordinates = "${project.group}:${project.name}:${project.version}"
      classloaderIsolationSpec!!.coordinates.convention(defaultCoordinates)
      classloaderIsolationSpec!!.configurationName.convention("gratatouille${defaultCoordinates.sanitize()}")
    }

    action.execute(classloaderIsolationSpec!!)

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
