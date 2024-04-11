package gratatouille.gradle

import com.google.devtools.ksp.gradle.KspExtension
import com.gradleup.gratatouille.gradle.BuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project

class GratatouilleImplementationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.withPlugins("org.jetbrains.kotlin.jvm", "com.google.devtools.ksp") {
            target.configurations.getByName("ksp").dependencies.add(
                target.dependencies.create("${BuildConfig.group}:gratatouille-processor:${BuildConfig.version}")
            )
            target.configurations.getByName("implementation").dependencies.add(
                target.dependencies.create("${BuildConfig.group}:gratatouille-core:${BuildConfig.version}")
            )


            target.afterEvaluate {
                target.extensions.getByName("ksp").apply {
                    this as KspExtension
                    this.arg("gratatouilleCoordinates", "${target.group}:${target.name}:${target.version}")
                }
            }
        }
    }
}
