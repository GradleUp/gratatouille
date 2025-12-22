package testplugin

import gratatouille.GExtension
import gratatouille.GPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class TestSettingsExtension1 {
    abstract val foo: Property<String>
}

@GPlugin(id = "testSettingsPlugin1")
fun testSettingsPlugin1(target: Settings) {
    target.extensions.create("testSettingsPlugin1", TestSettingsExtension1::class.java)
}


@GExtension(pluginId = "testSettingsPlugin2")
abstract class TestSettingsExtension2(val settings: Settings) {
    abstract val bar: Property<String>
}