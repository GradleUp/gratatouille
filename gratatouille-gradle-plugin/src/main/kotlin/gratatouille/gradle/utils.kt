package gratatouille.gradle

import org.gradle.api.Project


internal fun Project.withPlugins(vararg ids: String, block: () -> Unit) {
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