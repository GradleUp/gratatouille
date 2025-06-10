package testplugin

import gratatouille.wiring.GPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

@GPlugin("testplugin")
fun test(target: Project) {
    val extension = target.extensions.create("testExtension", TestExtension::class.java)

    val task1 = target.registerTaskAction1Task(
        stringInput = extension.stringInput,
        internalInput = extension.internalInput,
        optionalInput = extension.optionalInput,
        setInput = extension.setInput,
        listInput = extension.listInput,
        mapInput = extension.mapInput,
        filesInput = extension.filesInput,
        optionalFileInput = extension.optionalFileInput,
        fileInput = extension.fileInput,
        serializableInput = extension.serializableInput,
    )

    target.registerTaskAction2Task(
        input = extension.stringInput2,
        myData = task1.flatMap { it.outputFile }
    )

    target.registerTaskAction3Task(
        input = extension.stringInput3,
        outputFile = extension.fileOutput3
    )

    target.registerTaskAction4Task(
        input = extension.stringInput
    )
}

abstract class TestExtension {
    abstract val stringInput: Property<String>
    abstract val internalInput: Property<String>
    abstract val optionalInput: Property<String>

    abstract val setInput: SetProperty<Int>
    abstract val listInput: ListProperty<Int>
    abstract val mapInput: MapProperty<String, String>

    abstract val filesInput: ConfigurableFileCollection
    abstract val optionalFileInput: RegularFileProperty
    abstract val fileInput: RegularFileProperty
    abstract val serializableInput: RegularFileProperty

    abstract val stringInput2: Property<String>
    abstract val stringInput3: Property<String>

    abstract val fileOutput3: RegularFileProperty
}