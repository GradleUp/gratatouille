package gratatouille.gradle

import org.gradle.api.provider.Property

interface CodeGenerationExtension {
  /**
   * Enables experimental support for kotlinx serialization when generating tasks.
   */
  val enableKotlinxSerialization: Property<Boolean>

  /**
   * Automatically add dependencies needed for code generation if KSP is present.
   *
   * Default: true
   */
  var addDependencies: Boolean
}