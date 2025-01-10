package testplugin

import gratatouille.GExtension
import gratatouille.GPlugin
import org.gradle.api.Project


@GExtension(pluginId = "testplugin2")
abstract class TestExtension2(private val target: Project) {
  fun doStuff() {
    println("hello")
  }
}