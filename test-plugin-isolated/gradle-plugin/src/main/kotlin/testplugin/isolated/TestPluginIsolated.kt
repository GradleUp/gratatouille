package testplugin.isolated

import gratatouille.GPlugin
import org.gradle.api.Project
import org.gradle.api.provider.Property

@GPlugin(id = "testplugin.isolated")
fun testPluginIsolated(target: Project) {
    val extension = target.extensions.create("testExtensionIsolated", TestExtensionIsolated::class.java)

    val task1 = target.registerTaskActionIsolatedTask(
        stringInput = extension.stringInput,
    )
}


abstract class TestExtensionIsolated {
    abstract val stringInput: Property<String>
}