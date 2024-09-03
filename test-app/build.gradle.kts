import kotlin.test.assertFailsWith

plugins {
  id("testplugin")
  id("testplugin.isolated")
}

testExtension {
  stringInput.set("input")
  internalInput.set("internalInput")
  //optionalInput.set()

  setInput.add(42)
  listInput.add(43)
  mapInput.put("key", "value")

  filesInput.from(fileTree("inputs/fileInputs"))
  //optionalFileInput.set()
  fileInput.set(file("inputs/fileInput"))
  directoryInput.set(layout.projectDirectory.dir("inputs/directoryInput"))
  serializableInput.set(file("inputs/serializableInput"))

  stringInput2.set("input2")
  stringInput3.set("input3")

  fileOutput3.set(file("build/output"))
}

testExtensionIsolated {
  stringInput.set("world")
}

tasks.register("build") {
  dependsOn("taskAction2", "taskAction3", "taskActionIsolated")

  doLast {
    check(file("build/gtask/taskActionIsolated/outputFile").readText() == "hello world - okio.Buffer")

    // Make sure we don't have okio in the classpath
    assertFailsWith<ClassNotFoundException> {
      Class.forName("okio.Buffer")
    }
  }
}