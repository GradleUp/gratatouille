package gratatouille.gradle

import com.google.devtools.ksp.gradle.KspExtension
import com.gradleup.gratatouille.gradle.BuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project

class GratatouilleImplementationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.withRequiredPlugins("org.jetbrains.kotlin.jvm", "com.google.devtools.ksp") {
            target.configurations.getByName("ksp").dependencies.add(
                target.dependencies.create("${BuildConfig.group}:gratatouille-processor")
            )
            target.configurations.getByName("implementation").dependencies.add(
                target.dependencies.create("${BuildConfig.group}:gratatouille-core")
            )

            target.afterEvaluate {
                target.extensions.getByName("ksp").apply {
                    this as KspExtension
                    this.arg("implementationCoordinates", "${target.group}:${target.name}:${target.version}")
                }
            }
        }
        target.configureDefaultVersionsResolutionStrategy()
    }
}
