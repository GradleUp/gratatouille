plugins {
  id("testplugin.isolated").version("0.0.0")
}

testExtensionIsolated {
  this.stringInput.set("stringInput")
}

