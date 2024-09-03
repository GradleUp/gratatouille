package gratatouille.gradle

import com.gradleup.gratatouille.gradle.BuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project

class GratatouillePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.withPlugins("org.jetbrains.kotlin.jvm", "com.google.devtools.ksp") {
            target.configurations.getByName("ksp").dependencies.add(
                target.dependencies.create("${BuildConfig.group}:gratatouille-processor:${BuildConfig.version}")
            )
            target.configurations.getByName("implementation").dependencies.add(
                target.dependencies.create("${BuildConfig.group}:gratatouille-core:${BuildConfig.version}")
            )
        }
    }
}

