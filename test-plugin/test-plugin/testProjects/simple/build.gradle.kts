plugins {
  id("testplugin").version("0.0.0")
}

testExtension {
  this.mapInput.put("key", "value")
  this.listInput.add(42)
  this.setInput.add(43)
  this.filesInput.from(file("inputs/file0.txt"), fileTree("inputs/subdir"))
  this.fileInput.set(file("inputs/file0.txt"))
  this.fileOutput3.set(file("build/file3.txt"))
  this.internalInput.set("internalInput")
  this.stringInput.set("stringInput")
  this.stringInput2.set("stringInput2")
  this.stringInput3.set("stringInput3")
  this.serializableInput.set(file("inputs/input1.json"))
}

