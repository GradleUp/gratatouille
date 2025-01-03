package gratatouille.gradle

import com.gradleup.gratatouille.gradle.BuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project

class GratatouillePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("gratatouille", GratatouilleExtension::class.java, target)
        target.withRequiredPlugins("org.jetbrains.kotlin.jvm", "com.google.devtools.ksp") {
            target.configurations.getByName("ksp").dependencies.add(
                target.dependencies.create("${BuildConfig.group}:gratatouille-processor")
            )
            target.configurations.getByName("implementation").dependencies.add(
                target.dependencies.create("${BuildConfig.group}:gratatouille-core")
            )
        }
        target.configureDefaultVersionsResolutionStrategy()
    }
}

