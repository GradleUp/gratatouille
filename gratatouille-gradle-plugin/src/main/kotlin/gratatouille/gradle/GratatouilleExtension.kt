package gratatouille.gradle

interface GratatouilleExtension: CodeGenerationExtension {
  /**
   * Configures the Gradle API version required for the project.
   *
   * This method:
   * - Adds the Gradle API dependency to the `compileOnlyApi` configuration.
   *   - For 8.14 and above, adds `org.gradle.experimental:gradle-public-api` from https://repo.gradle.org/gradle/libs-releases.
   *   - For 8.13 and below, adds `dev.gradleplugins:gradle-api` from [nokee](https://docs.nokee.dev/manual/gradle-plugin-development.html#sec:gradle-dev-redistributed-gradle-api).
   * - Configures the Kotlin compiler version to match the target Gradle version as described in the [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)https://docs.gradle.org/current/userguide/compatibility.html.
   *
   * @param gradleVersion the Gradle version to target, specified as a string. Example: "8.14".
   * @param javaVersion the Java version to target, specified as an Int. Example: 21.
   */
  fun gradleTarget(gradleVersion: String, javaVersion: Int)

  /**
   * Configures the Gradle API version required for the project.
   *
   * This method:
   * - Adds the Gradle API dependency to the `compileOnlyApi` configuration.
   *   - For 8.14 and above, adds `org.gradle.experimental:gradle-public-api` from https://repo.gradle.org/gradle/libs-releases.
   *   - For 8.13 and below, adds `dev.gradleplugins:gradle-api` from [nokee](https://docs.nokee.dev/manual/gradle-plugin-development.html#sec:gradle-dev-redistributed-gradle-api).
   * - Configures the Kotlin compiler version to match the target Gradle version as described in the [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)https://docs.gradle.org/current/userguide/compatibility.html.
   * - Configures the Java compiler version to 17.
   *
   * @param gradleVersion the Gradle version to target, specified as a string. Example: "8.14".
   */
  fun gradleTarget(gradleVersion: String)

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
   * Calling this is not required if you are publishing with [Librarian](https://github.com/GradleUp/librarian/).
   *
   * @throws IllegalStateException if there are more
   */
  fun pluginMarker(id: String)

  /**
   * Creates a new `${pluginId}PluginMarkerMaven` publication allowing to locate the implementation coordinates from the plugin id.
   *
   * Calling this is not required if you are publishing with [Librarian](https://github.com/GradleUp/librarian/).
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

