package gratatouille.gradle

import com.google.devtools.ksp.gradle.KspExtension
import com.gradleup.gratatouille.gradle.BuildConfig
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension


internal fun Project.withRequiredPlugins(vararg ids: String, block: () -> Unit) {
    val pending = ids.toMutableSet()

    ids.forEach { pluginId ->
        pluginManager.withPlugin(pluginId) {
            pending.remove(pluginId)
            if (pending.isEmpty()) {
                block()
            }
        }
    }

    afterEvaluate {
        if (pending.isNotEmpty()) {
            error("Gratatouille requires the following plugin(s): '${pending.joinToString(",")}'")
        }
    }
}

internal fun Project.configureDefaultVersionsResolutionStrategy() {
    configurations.configureEach { configuration ->
        configuration.withDependencies { dependencySet ->
            val pluginVersion = VERSION
            dependencySet.filterIsInstance<ExternalDependency>()
                .filter { it.group == BuildConfig.group && it.version.isNullOrEmpty() }
                .forEach { it.version { constraint -> constraint.require(pluginVersion) } }
        }
    }
}

internal fun String.displayName() =
    this.split(".").joinToString(separator = "") { it.replaceFirstChar { it.uppercase() } }

internal val Project.kotlinExtension: KotlinJvmProjectExtension
    get() {
        val kotlin = project.extensions.findByType(KotlinJvmProjectExtension::class.java)
        check(kotlin != null) {
            "Gratatouille require the 'org.jetbrains.kotlin.jvm' plugin to be applied"
        }
        return kotlin
    }


internal val Project.kspExtension: KspExtension
    get() {
        val kotlin = project.extensions.findByType(KspExtension::class.java)
        check(kotlin != null) {
            "Gratatouille require the 'com.google.devtools.ksp' plugin to be applied"
        }
        return kotlin
    }

internal val USAGE_GRATATOUILLE = "gratatouille"
