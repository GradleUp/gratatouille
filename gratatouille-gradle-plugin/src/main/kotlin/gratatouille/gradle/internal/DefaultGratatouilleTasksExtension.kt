package gratatouille.gradle.internal

import gratatouille.gradle.GratatouilleTasksExtension
import gratatouille.GExtension
import org.gradle.api.Project

@Suppress("unused")
@GExtension("com.gradleup.gratatouille.tasks", "gratatouille", GratatouilleTasksExtension::class)
abstract class DefaultGratatouilleTasksExtension(project: Project): GratatouilleTasksExtension {
  init {
    addDependencies = true
    val defaultCoordinates = project.provider { "${project.group}:${project.name}:${project.version}" }
    coordinates.convention(defaultCoordinates)
    configurationName.convention(defaultCoordinates.map { "gratatouille${it.sanitize()}" })

    project.configureCodeGeneration(this)
  }
}
