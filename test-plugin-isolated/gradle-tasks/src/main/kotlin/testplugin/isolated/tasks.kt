package testplugin.isolated

import gratatouille.*
import okio.buffer
import okio.sink

@GTask
internal fun taskActionIsolated(
    stringInput: String,
    outputFile: GOutputFile
) {
    outputFile.sink().buffer().use {
        it.writeUtf8("hello $stringInput - ${Class.forName("okio.Buffer").name}")
    }
}
