# Gratatouille 🐘🤝🐭👉🧑‍🍳 

Gratatouille is an opinionated framework to build Gradle plugins. Write Kotlin functions and the Gratatouille KSP processor generates tasks, workers, and wiring code for you.

When used in classloader isolation mode, Gratatouille enforces a clear separation between your plugin implementation (**tasks**) and your plugin glue (**wiring**) making your tasks immune to [classloader](https://github.com/square/kotlinpoet/issues/1730#issuecomment-1819118527) [issues](https://github.com/gradle/gradle/issues/1370) 🛡️ 

**Key Features**:

* [Tasks Generation](#functions)
* [Comprehensive input/output types](#supported-input-and-output-types)
* [Non overlapping task outputs by default](#non-overlapping-task-outputs-by-default)
* [Build cache by default](#build-cache-by-default)
* [Easy documentation](#easy-documentation)
* [Parallel execution by default](#parallel-task-execution-by-default)
* [Compile-time task wiring](#compile-time-task-wiring)
* [Plugin descriptors and markers without java-gradle-plugin](#descriptors-and-markers)
* [Classloader isolation](#classloader-isolation-optional) (optional)
* [kotlinx serialization support](#experimental-kotlinxserialization-support) (experimental)

Check out the [Apollo Faker Gradle Plugin](https://github.com/apollographql/apollo-kotlin-faker/blob/main/gradle-plugin/build.gradle.kts) for a real life example or [test-app](test-app) for integration tests.

Gratatouille also uses Gratatouille to build its plugin, check [gratatouille-gradle-plugin/build.gradle.kts](gratatouille-gradle-plugin/build.gradle.kts) for more details. 

## Quick Start

Apply the `com.gradleup.gratatouille` plugin:

```kotlin
plugins {
  id("org.jetbrains.kotlin.jvm")
  // KSP is required for code generation
  id("com.google.devtools.ksp")
  // No need to add the 'java-gradle-plugin' plugin.
  // Add the Gratatouille plugin
  id("com.gradleup.gratatouille").version("0.1.1")
}

gratatouille {
  // Configure the plugin marker
  pluginMarker("com.example.myplugin")
  // Enable code generation
  codeGeneration()
}
```

Define your task action using `@GTask`:

```kotlin
@GTask
internal fun prepareIngredients(persons: Int, ingredients: GOutputFile) {
  ingrediens.writeText("""
  {
    "tomatoes": ${persons * 0.75}.roundToInt(),
    "zucchinis": ${persons * 0.3}.roundToInt(),
    "eggplants": ${persons * 0.3}.roundToInt()
  }
  """.trimIndent())  
}
```
Gratatouille automatically maps function parameters to Gradle inputs and outputs(more on outputs [below](#non-overlapping-task-outputs-by-default)).

Gratatouille generates entry points, tasks, workers and [the rest of the owl](https://ibin.co/8lrGHjKW7mig.png)!

<details>
<summary>Generated code</summary>

```kotlin
internal fun Project.registerPrepareIngredientsTask(
  taskName: String = "prepareIngredients",
  taskDescription: String? = null,
  taskGroup: String? = null,
  persons: Provider<Int>,
): TaskProvider<PrepareIngredientsTask> {
  val configuration = this@registerPrepareIngredientsTask.configurations.detachedConfiguration()
  configuration.dependencies.add(dependencies.create("sample-plugin:implementation:0.0.1"))
  return tasks.register(taskName,PrepareIngredientsTask::class.java) {
    it.description = taskDescription
    it.group = taskGroup
    it.classpath.from(configuration)
    // infrastructure
    // inputs
    it.persons.set(persons)
    // outputs
    it.outputFile.set(this@registerPrepareIngredientsTask.layout.buildDirectory.file("gtask/${taskName}/ingredients"))
  }
}

@CacheableTask
internal abstract class PrepareIngredientsTask : DefaultTask() {
  @get:Classpath
  public abstract val classpath: ConfigurableFileCollection

  @get:Input
  public abstract val persons: Property<Int>

  @get:OutputFile
  public abstract val outputFile: RegularFileProperty

  @Inject
  public abstract fun getWorkerExecutor(): WorkerExecutor

  private fun <T> T.isolate(): T {
    @kotlin.Suppress("UNCHECKED_CAST")
    when (this) {
        is Set<*> -> {
            return this.map { it.isolate() }.toSet() as T
        }

        is List<*> -> {
            return this.map { it.isolate() } as T
        }

        is Map<*, *> -> {
            return entries.map { it.key.isolate() to it.value.isolate() }.toMap() as T
        }

        else -> {
            return this
        }
    }
  }

  @TaskAction
  public fun taskAction() {
    getWorkerExecutor().noIsolation().submit(PrepareIngredientsWorkAction::class.java) {
      it.classpath = classpath.files.isolate()
      it.persons = persons.get().isolate()
      it.ingredients = ingredients.asFile.get().isolate()
    }
  }
}

private interface PrepareIngredientsWorkParameters : WorkParameters {
  public var classpath: Set<File>

  public var persons: Int

  public var ingredients: File
}

private abstract class PrepareIngredientsWorkAction : WorkAction<PrepareIngredientsWorkParameters> {
  override fun execute() {
    with(parameters) {
      URLClassLoader(
        classpath.map { it.toURI().toURL() }.toTypedArray(),
        ClassLoader.getPlatformClassLoader()
      ).loadClass("recipes.PrepareIngredientsEntryPoint")
      .declaredMethods.single()
      .invoke(
        null,
        persons,
        ingredients,
      )
    }
  }
}

public class PrepareIngredientsEntryPoint {
  public companion object {
    @JvmStatic
    public fun run(persons: Int, ingredients: File) {
      prepareIngredients(
        persons = persons,
        ingredients = ingredients,
      )
    }
  }
}
```
</details> 

Use `@GPlugin` to create a plugin and call `Project.register${TaskAction}Task()` to register the task:

```kotlin
// GPlugin generates a plugin and descriptor automatically
@GPlugin(id = "com.example.myplugin")
fun myPlugin(project: Project) {
    val extension = project.extensions.create("recipes", RecipesExtension::class.java)

    // Register your "PrepareIngredients" task
    val prepareIngredients = project.registerPrepareIngredientsTask(
        persons = extension.persons
        // no need to set the outputs, they are configured automatically 
    )
    
    // Register other tasks
    project.registerCookTask(
        recipe = extension.recipe,
        // Wire tasks together
        ingredients = prepareIngredients.flatMap { it.ingredients }
    )
}
```

No need to implement `DefaultTask`, no risk of forgetting `@Cacheable`, etc... Gratatouille provides good defaults making it easier to write plugins.

## Features

### Functional programming style

Your code is modeled as functions taking inputs and generating outputs. 

No need for stateful properties or classes. Nullable parameters are generated as optional task properties. Calls to `Provider.get()` or `Provider.orNull` are automated.

### Supported input and output types

Inputs:
* Any type annotated with `@Serializable` (serialized to a File)
* Kotlin `Int`, `Boolean`, `Float`, `Double`, `String`
* Kotlin `Set`, `List`, `Map`
* Single File using the `GInputFile` typealias
* FileCollection using the `GInputFiles` typealias
* Directory using the `GInputDirectory` typealias

Outputs:
* Single File using the `GOutputFile` typealias
* Directory using the `GOutputDirectory` typealias

No need to dive into [the details of `@InputDirectoy`](https://mbonnin.net/2025-01-22_input_directory_is_a_lie/) or [`PathSensitivity`](https://www.linen.dev/s/gradle-community/t/29202922/hey-i-have-a-simple-question-famous-last-words-i-think-it-is#892fd930-9a5d-4471-bd9a-acae3e61e518), Gratatouille input and output types support a wide range of use case with a clear and restricted API.

### Non-overlapping task outputs by default

Gratatouille allocates paths for output files and directories automatically. Each output gets a dedicated filesystem location at `"build/gtask/${taskName}/${outputName}"`. 

This way:
* you don't have to think about what path to use.
* the outputs are consistent and discoverable.
* issues like [#26091](https://github.com/gradle/gradle/issues/26091) are avoided by construction.

If your function has a single return value, Gratatouille uses `outputFile` as output name.

If your function needs multiple return values, wrap them in a non-serializable class.

If you need to control the output location of an output, you can do so using `@GManuallyWired` and using `GOutputFile`/`GOutputDirectory` as parameters.

In your implementation:

```kotlin
@GTask
internal fun cook(
    recipe: GInputFile,
    ingredients: Ingredients,
    // ratatouille is exposed in registerCookTask(outputFile) so you can configure it 
    @GManuallyWired ratatouille: GOutputFile,
    // leftovers is set to "build/gtask/cook/leftovers" 
    leftovers: GOutputFile,
) {
    ratatouille.writeText(/* cook here! */)
}
```

In your plugin:

```kotlin
project.registerCookTask(
    recipe = extension.recipe,
    ingredients = prepareIngredients.flatMap { it.outputFile },
    // Set outputFile location explicitly
    ratatouille = project.layout.buildDirectory.file("ratatouille")
    // No need to set lefovers
)
```

### Build cache by default

`@CacheableTask` is added by default. All input files use `PathSensitivity.RELATIVE` making your tasks relocatable. 

### Easy documentation

`@GTask` takes a `description` and a `group` argument making it easy to colocate your documentation with your implementation:

```kotlin
@GTask(
    description = "cooks the most delicious ratatouille with the help of the tiniest chef",
    group = "recipes"
)
internal fun cook(
    recipe: GInputFile,
    ingredients: Ingredients,
    outputFile: GOutputFile
) { 
    TODO()
}
```

### Parallel task execution by default

By default, [Gradle tasks execute serially in a single project](https://docs.gradle.org/current/userguide/performance.html#parallel_execution) (unless using the [configuration cache](https://docs.gradle.org/current/userguide/performance.html#additional_configuration_cache_benefits)).

Because your task actions are Kotlin functions, no state is shared, making them perfect candidates for parallelization. 

Gratatouille uses the [Worker API](https://docs.gradle.org/current/userguide/worker_api.html) to allow parallel execution making your build faster overall. Use `org.gradle.workers.max` to control the maximum number of workers.

### Compile time task wiring

Finally, Gratatouille encourages exposing extensions to users instead of task classes directly. All generated code is generated as `internal`. This makes it easier to have some inputs user configurable while some others are an implementation details and more generally makes it easier to evolve the public API of your plugin.

When a task has a high number of inputs, it can become hard to track which ones have been wired and which ones haven't. By using a central registration point, Gratatouille enforces at build time that all inputs/outputs have been properly wired.

## Descriptors and markers

In order to map a plugin id to a jar file and a specific implementation class, Gradle uses [plugin markers](https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_markers) and descriptors.

The markers and descriptors are typically added by the [`java-gradle-plugin`](https://docs.gradle.org/current/userguide/java_gradle_plugin.html) plugin. This plugin also adds the `gradleApi()` dependency as an `api` dependency to your project, which is rarely needed.

For simplicity, Gratatouille, generates plugin descriptors automatically from the `@GPlugin` and `GExtension` annotations. 

For markers, Gratatouille exposes a simple function:

```kotlin
gratatouille {
  // Configure the plugin marker
  pluginMarker("com.example.myplugin")
}

dependencies {
  // You can add use the Gradle API of your choice here 
  compileOnly("dev.gradleplugins:gradle-api:8.0")
}
```

## Classloader isolation (optional)

Gradle uses [multiple classloaders](https://dev.to/autonomousapps/build-compile-run-a-crash-course-in-classpaths-f4g), and it's notoriously really hard to understand where a given class is loaded from.

Especially, `buildSrc`/`build-logic` dependencies [leak in the main classpath](https://github.com/gradle/gradle/issues/4741) and override any dependencies from other plugin without conflict resolution. There are multiple workarounds such as declaring all plugins in `buildSrc` or in the top level `build.gradle[.kts]` file but the situation is confusing to Gradle newcomers and hard to debug.

To guard against those issues, Gratatouille provides a classloader isolation mode where your tasks use a separate classloader.

This means your tasks can depend on popular dependencies such as the Kotlin stdlib, KotlinPoet or ASM without risking conflicts with other plugins or the Gradle classpath itself.

> [!TIP]
> As an added bonus, isolating your tasks makes your build generally more up-to-date. Changing the implementation of a task doesn't invalidate your whole build anymore. Enjoy faster CI times and more granular task invalidation!

For classloader isolation to work, your plugin needs 2 projects:
* The **tasks** project is where the task actions are defined and the work is done. This project can add dependencies. 
* The **plugin** project contains the glue code and Gradle API that calls the **tasks** project through reflection. This project must not add dependencies besides the compileOnly Gradle API.

### Step 1/2: gradle-tasks

Create a `gradle-tasks` project for your plugin tasks and apply the `com.gradleup.gratatouille.tasks` plugin:

```kotlin
// implementation/build.gradle.kts
plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.gradleup.gratatouille.tasks").version("0.1.1")
}

dependencies {
    // Add dependencies needed to do your task work
    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation("org.ow2.asm:asm-commons:9.6")
    // do **not** add gradleApi() here
}

gratatouille {
  // Enable code generation
  codeGeneration {
    // Enables classloader isolation
    classLoaderIsolation()
  }
}
```

Write your task actions as top-level Kotlin functions annotated with `@GTask`:

```kotlin
@GTask
internal fun prepareIngredients(persons: Int, ingredients: GOutputFile) {
  ingrediens.writeText("""
  {
    "tomatoes": ${persons * 0.75}.roundToInt(),
    "zucchinis": ${persons * 0.3}.roundToInt(),
    "eggplants": ${persons * 0.3}.roundToInt()
  }
  """.trimIndent())
}
```

When using this mode, the plugin wiring code is generated as resources that are included by the `gradle-plugin` project. 

### Step 2/2 gradle-plugin

To use the generated code in your plugin, create an `gradle-plugin` project next to your `gradle-tasks` project.

> [!IMPORTANT]
> By using two different projects, Gratatouille ensures that Gradle classes do not leak in your plugin implementation and vice-versa.

Apply the `com.gradleup.gratatouille.wiring` plugin in your `gradle-plugin` project:

```kotlin
// gradle-plugin/build.gradle.kts
plugins {
    id("com.gradleup.gratatouille.wiring").version("0.1.1")
}

gratatouille {
  // Configure the plugin marker
  pluginMarker("com.example.myplugin")
  // Optional: you may still use code generation for `@GTask` and `GExtension` helpers
  codeGeneration()
}

dependencies {
  // Add your implementation project to the "gratatouille" configuration.
  // This does not add `:implementation` to your plugin classpath.
  // Instead, the generated code uses reflection and a separate classloader to run
  // your implementation
  gratatouille(project(":implementation"))
  
  // Add the version of Gradle you want to compile against 
  compileOnly("dev.gradleplugins:gradle-api:8.0")
}

```

In your plugin code, use `Project.register${TaskAction}Task()` to register the task

### Experimental kotlinx.serialization support

Gratatouille has builtin support for [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization). Models are serialized and deserialized as needed.

To opt-in support for kotlinx.serialization, add `enableKotlinxSerialization.set(true)` to your configuration:

```kotlin
gratatouille {
  enableKotlinxSerialization.set(true)
  codeGeneration()
  // ...
}
```

With `kotlinx.serialization` support, you can write your funtions as pure functions and the output will be serialized on the fly:

```kotlin
@GTask
internal fun prepareIngredients(persons: Int): Ingredients {
    return Ingredients(
        tomatoes = (persons * 0.75).roundToInt(),
        zucchinis = (persons * 0.3).roundToInt(),
        eggplants = (persons * 0.3).roundToInt(),
    )
}

// kotlinx.serialization is supported out of the box
@Serializable
internal data class Ingredients(
    val tomatoes: Int,
    val zucchinis: Int,
    val eggplants: Int,
)
```
