# Next version (unreleased)

# 0.2.1
_2025_12_25_

Hotfix release to make it possible for plugins built with Gratatouille > 0.2.0 to work with the ones built with Gratatouille < 0.2.0 (#90)

# 0.2.0 
_2025_12_22_

This release simplifies the Gradle configuration and adds support for generating `Settings` plugins, alongside a few other fixes.

## `com.gradleup.gratatouille.wiring` is replaced by `com.gradleup.gratatouille` for simplicity.

```kotlin 
plugins {
  // Replace 
  id("com.gradleup.gratatouille.wiring")
  // With
  id("com.gradleup.gratatouille")
}
```

Similarly, `gratatouille-wiring-runtime` is now just `gratatouille-runtime`:

```kotlin 
dependencies {
  // Replace 
  implementation("gratatouille-wiring-runtime")
  // With
  implementation("gratatouille-runtime")
}
```

You'll also need to replace the package:

```kotlin
// Replace
import gratatouille.wiring.*
// With
import gratatouille.*
```

Code generation is now enabled automatically if KSP is applied:

```kotlin
gratatouille {
  // Remove
  codeGeneration()
  
  // If you need to configure some parameters, those are now top level:
  
  // Replace
  codeGeneration {
    addDependencies = false
  }
  // With
  addDependencies = false
}
```

👷‍♂️ All changes:

* [NEW] Add support for settings plugins in https://github.com/GradleUp/gratatouille/pull/87
* [BREAKING] Merge the `com.gradleup.gratatouille.wiring` and the `com.gradleup.gratatouille` plugins and simplifiy configuration in https://github.com/GradleUp/gratatouille/pull/84
* [FIX] Allow digits in the isolated classloader configuration name in https://github.com/GradleUp/gratatouille/pull/80
* [FIX] Remove json dependency in the wiring runtime in https://github.com/GradleUp/gratatouille/pull/82
* [FIX] Change configurations.all to configurations.configureEach in https://github.com/GradleUp/gratatouille/pull/81



# Version 0.1.3
_2025_11_04_

Maintenance release.

Brings back `group` and `description` in generated code because it's easy to configure everything in one place, add `pluginLocalPublication()` to make Gratatouille generated plugins work better when used in included builds and more lazy API usages courtesy of @simonlebras, many thanks 🙏

## All changes
* Use a named configuration for the tasks dependencies by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/71
* Revert "Remove group and description (#69)" by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/72
* Do not add path sensitivity annotations for internal properties by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/73
* Do not include a timestamp in plugin markers by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/74
* Add GratatouilleExtension.pluginLocalPublication() for included builds compatibility by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/75
* Use lazy configuration for classpath by @simonlebras in https://github.com/GradleUp/gratatouille/pull/77

# Version 0.1.2 
_2025_08_27_

* Fix a bug introduced in #55 where filtering the input files would lose the normalized path (#68)
* Remove `group` and `description` from the codegen. It is very easy to set those outside of Gratatouille (#69)

# Version 0.1.1
_2025-08-20_

* [NEW] Use logger API to report errors by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/60
* [NEW] Add support for BuildServices in parameters (they still need to be registered ahead of time) by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/63
* [NEW] Allow to use GPlugin for settings plugins by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/66
* [FIX] Filter out directories from `GInputFiles` by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/55
* [FIX] Allow empty package name in plugin classes by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/57
* [FIX] Reserve 'project' name by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/59

# Version 0.1.0
_2025-06-30_

* Update boostrap version by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/41
* Add a classifier and extension for generated gratatouille files by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/52
* Update Librarian and Nmcp versions by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/53

# Version 0.0.10
_2025-06-11_

* Cache classloaders in a build service (#39)
* Split the plugin in 3 different parts:
  * `com.gradleup.gratatouille` for simple cases.
  * `com.gradleup.gratatouille` and `com.gradleup.gratatouille.tasks` for classloader isolation cases.
* Similarly, the runtimes are split:
  * `gratatouille-wiring-runtime` for the wiring.
  * `gratatouille-tasks-runtime` for the tasks.

# Version 0.0.9
_2025-05-31_

* Add more log levels (#36)
* Add GClasspath (#37)

# Version 0.0.8
_2025-05-14_

* Remove kotlin top-level property by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/32
* Multiple QOL improvements from trying to integrate Gratatouille in Apollo Kotlin by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/34
* update publication to use the central portal by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/35

# Version 0.0.7
_2025-04-25_

* Technical release to remove dependency on SNAPSHOT artifacts

# Version 0.0.6
_2025-04-25_

* Use `@Classpath` for classpath properties (#29)
* Add support for injecting logger in task actions (#28)

# Version 0.0.5
_2025-04-16_

* Added `@GPlugin`, `@GExtension` and `@GFileName`
* Made kotlinx.serialization support opt-in
* Simplify Gradle configuration with only one plugin

# Version 0.0.4
_2024-09-03_

* Fix isolation of input files

# Version 0.0.3
_2024-09-03_

* Added `com.gradleup.gratatouille` for `buildSrc` and other use cases where classloader isolation isn't required.
* Expose the normalized path in `GInputFiles`
