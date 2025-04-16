import testplugin.TestExtension
import testplugin.isolated.TestExtensionIsolated
import kotlin.test.assertFailsWith

buildscript {
  dependencies {
    classpath("test-plugin:gradle-plugin")
    classpath("test-plugin-isolated:gradle-plugin")
  }
  repositories {
    mavenCentral()
    maven("https://storage.googleapis.com/gradleup/m2")
  }
}

plugins.apply("testplugin")
plugins.apply("testplugin.isolated")

extensions.getByName("testExtension").apply {
  this as TestExtension
  stringInput.set("input")
  internalInput.set("internalInput")
  //optionalInput.set()

  setInput.add(42)
  listInput.add(43)
  mapInput.put("key", "value")

  filesInput.from(fileTree("inputs/fileInputs"))
  //optionalFileInput.set()
  fileInput.set(file("inputs/fileInput"))
  serializableInput.set(file("inputs/serializableInput"))

  stringInput2.set("input2")
  stringInput3.set("input3")

  fileOutput3.set(file("build/output"))
}

extensions.getByName("testExtensionIsolated").apply {
  this as TestExtensionIsolated
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