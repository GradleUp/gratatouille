# Next version (unreleased)

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
