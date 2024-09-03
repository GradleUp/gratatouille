package testplugin.isolated

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import testplugin.isolated.registerTaskActionIsolatedTask

class TestPluginIsolated : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("testExtensionIsolated", TestExtensionIsolated::class.java)

        val task1 = target.registerTaskActionIsolatedTask(
            stringInput = extension.stringInput,
        )
    }
}

abstract class TestExtensionIsolated {
    abstract val stringInput: Property<String>
}