package testplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty

class TestPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val task1 = target.registerTaskAction1Task(
            stringInput = target.provider { "stringInput" },
            internalInput = target.provider { "internalInput" },
            optionalInput = target.objects.property(String::class.java),
            setInput = target.provider { setOf(0) },
            listInput = target.provider { listOf(0) },
            mapInput = target.provider { mapOf("key" to "value") },
            filesInput = target.files("inputs/filesInput"),
            optionalFileInput = target.objects.fileProperty(),
            fileInput = target.provider { target.layout.projectDirectory.file("inputs/fileInput") },
            directoryInput = target.provider { target.layout.projectDirectory.dir("inputs/directoryInput") },
            serializableInput = target.provider { target.layout.projectDirectory.file("inputs/serializableInput") },
        )

        target.registerTaskAction2Task(
            input = target.provider { "input2" },
            myData = task1.flatMap { it.outputFile }
        )

        target.registerTaskAction3Task(
            input = target.provider { "input3" },
            outputFile = target.layout.buildDirectory.file("output3")
        )
    }
}