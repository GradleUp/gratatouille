package gratatouille.gradle

import org.gradle.api.Action
import org.gradle.api.provider.Property

interface CodeGenerationSpec {
  /**
   * Enables experimental support for kotlinx.
   */
  val enableKotlinxSerialization: Property<Boolean>

  /**
   * Adds dependencies needed for code generation
   * Default: true
   */
  val addDependencies: Property<Boolean>

  /**
   * Enables classloader isolation
   */
  fun classLoaderIsolation()

  /**
   * Enables classloader isolation
   */
  fun classLoaderIsolation(action: Action<ClassloaderIsolationSpec>)
}