package gratatouille.gradle

import org.gradle.api.provider.Property

interface GratatouilleTasksExtension: CodeGenerationExtension {
  /**
   * The coordinates of the artifact where the task implementations are in the form group:artifact:version.
   *
   * Default: project.group:project.name:project.version
   */
  val coordinates: Property<String>

  /**
   * Unique name to identify the configuration
   *
   * Defaults to a name based on [coordinates]
   */
  val configurationName: Property<String>
}