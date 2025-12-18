package gratatouille.gradle

interface GratatouilleExtension: CodeGenerationExtension {
  /**
   * Registers a `generate${pluginId}Descriptor` task that generates a [plugin descriptor](https://docs.gradle.org/current/userguide/java_gradle_plugin.html#sec:gradle_plugin_dev_usage) for the plugin
   * and wires it to the `processResources` task.
   *
   * The descriptor will be included in the .jar file making it possible to locate the plugin implementation from the plugin id.
   *
   * Calling this function is usually not needed if using code generation as code generation can use `@GPlugin` to get the plugin id.
   *
   * @param implementationClass the fully qualified class name for the plugin implementation. Example: `com.example.ExamplePlugin`.
   */
  fun pluginDescriptor(id: String, implementationClass: String)

  /**
   * Creates a new `${pluginId}PluginMarkerMaven` publication allowing to locate the implementation coordinates from the plugin id.
   * This method requires that only a single publication is present in this project. See other overloads if you need more control over publications.
   *
   * @throws IllegalStateException if there are more
   */
  fun pluginMarker(id: String)

  /**
   * Creates a new `${pluginId}PluginMarkerMaven` publication allowing to locate the implementation coordinates from the plugin id.
   *
   * @param mainPublication the publication to redirect to.
   */
  fun pluginMarker(id: String, mainPublication: String)

  /**
   * Registers a local plugin publication so that the plugin is discoverable from included builds.
   *
   * This function uses Gradle internal APIs.
   *
   * @param id the plugin id
   */
  fun pluginLocalPublication(id: String)
}

