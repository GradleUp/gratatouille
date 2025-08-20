# Next version (unreleased)

# Version 0.1.1
_2025-08-20_

* [NEW] Use logger API to report errors by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/60
* [NEW] Add support for BuildServices in parameters (they still need to be registered ahead of time) by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/63
* [NEW] Allow to use GPlugin for settings plugins by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/66
* [FIX] Filter out directories from `GInputFiles` by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/55
* [FIX] Allow empty package name in plugin classes by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/57
* [FIX] Reserve 'project' name by @martinbonnin in https://github.com/GradleUp/gratatouille/pull/59

# Version 0.0.10
_2025-06-11_

* Cache classloaders in a build service (#39)
* Split the plugin in 3 different parts:
  * `com.gradleup.gratatouille` for simple cases.
  * `com.gradleup.gratatouille.wiring` and `com.gradleup.gratatouille.tasks` for classloader isolation cases.
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
