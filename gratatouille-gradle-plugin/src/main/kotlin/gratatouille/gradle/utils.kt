package gratatouille.gradle

import com.gradleup.gratatouille.gradle.BuildConfig
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency


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
        // Use the API introduced in Gradle 4.4 to modify the dependencies directly before they are resolved:
        configuration.withDependencies { dependencySet ->
            val pluginVersion = VERSION
            dependencySet.filterIsInstance<ExternalDependency>()
                .filter { it.group == BuildConfig.group && it.version.isNullOrEmpty() }
                .forEach { it.version { constraint -> constraint.require(pluginVersion) } }
        }
    }
}