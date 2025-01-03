package testplugin

import gratatouille.*
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Input1(val value: String)

@Serializable
data class MyData(val value: String)

@Serializable
data class Output21(val value: String)

@Serializable
data class Output22(val value: String)

class TaskAction2Output(val output21: Output21, val output22: Output22)


@GTaskAction
internal fun taskAction1(
    stringInput: String,
    @GInternal
    internalInput: String,
    optionalInput: String?,
    setInput: Set<Int>,
    listInput: List<Int>,
    mapInput: Map<String, String>,
    fileInput: GInputFile,
    optionalFileInput: GInputFile?,
    filesInput: GInputFiles,
    directoryInput: GInputDirectory,
    serializableInput: Input1
): MyData {
    return buildString {
        appendLine("stringInput: $stringInput")
        appendLine("internalInput: $internalInput")
        appendLine("optionalInput: $optionalInput")
        appendLine("setInput: $setInput")
        appendLine("listInput: $listInput")
        appendLine("mapInput: $mapInput")
        appendLine("fileInput.length(): ${fileInput.length()}")
        appendLine("optionalFileInput: ${optionalFileInput?.length()}")
        appendLine("filesInput.count(): ${filesInput.count()}")
        appendLine("filesInput: ${filesInput.map { it.normalizedPath }}")
        appendLine("directoryInput.walk().count(): ${directoryInput.walk().count()}")
        appendLine("serializableInput: $serializableInput")
    }.let {
        MyData(it)
    }
}

@GTaskAction
internal fun taskAction2(
    input: String,
    myData: MyData
): TaskAction2Output {
    return TaskAction2Output(
        output21 = Output21(input + "1" + myData.value),
        output22 = Output22(input + "2")
    )
}

@GTaskAction
internal fun taskAction3(
    input: String,
    @GManuallyWired
    outputFile: GOutputFile,
    outputDirectory: GOutputDirectory,
) {
    outputFile.writeText("$input in file")
    outputDirectory.resolve("output.txt").writeText("$input in file in dir")
}

@GTaskAction(pure = false)
internal fun taskAction4(
    input: String,
    outputFile: GOutputFile,
) {
    outputFile.writeText("${Date()}: $input in file")
}