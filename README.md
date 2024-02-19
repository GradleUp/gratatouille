# Gratatouille 🐘🤝🐭👉🧑‍🍳 

Gratatouille is an opinionated framework to build Gradle plugins. Write pure Kotlin functions and the Gratatouille KSP processor generates tasks, workers and wiring code for you.

Gratatouille enforces a clear separation between your plugin logic (**implementation**) and your plugin wiring (**gradle-plugin**) making your plugin immune to classloader issues 🛡️ 

**Features**:
* [Pure functions](#pure-functions)
* [Kotlinx serialization support](#built-in-kotlinxserialization-support)
* [Comprehensive input/output types](#supported-input-and-output-types)
* [Non overlapping task outputs](#non-overlapping-task-outputs-by-default)
* [Classloader isolation](#classloader-isolation-by-default)
* [Build cache](#build-cache-by-default)
* [Documentation](#easy-documentation)
* [Parallel execution](#parallel-task-execution-by-default)
* [Compile-time task wiring](#compile-time-task-wiring)

Check out the [sample-plugin](sample-plugin) and [sample-app](sample-app).

# Quick Start

## Step 1/2: `com.gradleup.gratatouille.implementation` 

Create an `implementation` module for your plugin implementation and apply the `com.gradleup.gratatouille.implementation` plugin:

```kotlin
// implementation/build.gradle.kts
plugins {
    id("com.gradleup.gratatouille.implementation").version("0.0.1-SNAPSHOT")
}

dependencies {
    // Add the gratatouille annotations
    implementation("com.gradleup.gratatouille:gratatouille-core:0.0.1-SNAPSHOT")
    // Add other dependencies
    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation("org.ow2.asm:asm-commons:9.6")
    // do **not** add gradleApi() here
}
```

Write your task action as a pure top-level Kotlin function annotated with `@GTaskAction`:

```kotlin
@GTaskAction
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

Gratatouille automatically maps function parameters to Gradle inputs and the return value to a Gradle output (more on outputs [below](#non-overlapping-task-outputs-by-default)).

Gratatouille generates entry points, tasks, workers and Gradle wiring code that you can then use to cook your plugin.

## Step 2/2 `com.gradleup.gratatouille.plugin` 

To use the generated code in your plugin, create a `gradle-plugin` module next to your `implementation` module. 

> [!IMPORTANT]
> By using two different modules, Gratatouille ensures that Gradle classes do not leak in your plugin implementation and vice-versa.

Apply the `com.gradleup.gratatouille.plugin` plugin in your `gradle-plugin` module:

```kotlin
// gradle-plugin/build.gradle.kts
plugins {
    id("java-gradle-plugin")
    id("com.gradleup.gratatouille.plugin").version("0.0.1-SNAPSHOT")
}

dependencies {
    // Add your implementation module to the "gratatouille" configuration.
    // This does not add `:implementation` to your plugin classpath.
    // Instead, the generated code uses reflection and a separate classloader to run
    // your implementation
    gratatouille(project(":implementation"))
}

// Create your plugin as usual, see https://docs.gradle.org/current/userguide/java_gradle_plugin.html 
gradlePlugin {
    // ... 
}
```

In your plugin code, use `Project.register${TaskAction}Task()` to register the task:

```kotlin
override fun apply(project: Project) {
    val extension = project.extensions.create("recipes", RecipesExtension::class.java)

    // Register your "PrepareIngredients" task
    val prepareIngredients = project.registerPrepareIngredientsTask(
        persons = extension.persons
    )
    
    // Register other tasks
    project.registerCookTask(
        recipe = extension.recipe,
        // Wire tasks together
        ingredients = prepareIngredients.flatMap { it.outputFile }
    )
}
```

# Features

## Pure functions

Your task code is a side-effect-free function, making it easier to [parallelize](#parallel-task-execution-by-default) and reason about. 

Nullable parameters are generated as optional task properties. Calls to `Provider.get()` or `Provider.orNull` are automated.

## Built-in kotlinx.serialization support

Gratatouille has builtin support for [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization). Models are serialized and deserialized as needed.

## Supported input and output types

Inputs:
* Any type annotated with `@Serializable` (serialized to a File)
* Kotlin `Int`, `Boolean`, `Float`, `Double`, `String`
* Kotlin `Set`, `List`, `Map`
* Single File using the `GInputFile` typealias
* FileCollection using the `GInputFiles` typealias
* Directory using the `GInputDirectory` typealias

Outputs:
* Any type annotated with `@Serializable` (serialized to a File)
* Single File using the `GOutputFile` typealias
* Directory using the `GOutputDirectory` typealias

## Non-overlapping task outputs by default

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
@GTaskAction
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

## Classloader isolation by default

Gratatouille creates a separate classloader for each task and calls your pure functions using reflection. 

This means your plugin can depend on popular dependencies such as the Kotlin stdlib, KotlinPoet or ASM without risking conflicts with other plugins or the Gradle classpath itself. 

## Build cache by default

`@CacheableTask` is added by default. All input files use `PathSensitivity.RELATIVE` making your tasks relocatable.

## Easy documentation

`@GTaskAction` takes a `description` and a `group` argument making it easy to colocate your documentation with your implementation:

```kotlin
@GTaskAction(
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

## Parallel task execution by default

Even with `org.gradle.parallel=true`, [Gradle tasks execute serially in a single module](https://docs.gradle.org/current/userguide/performance.html#parallel_execution).

Because your task actions are pure Kotlin function, no state is shared, making them perfect candidates for parallelization. 

Gratatouille uses the [Worker API](https://docs.gradle.org/current/userguide/worker_api.html) to allow parallel execution making your build faster overall. Use `org.gradle.workers.max` to control the maximum number of workers.

## Compile time task wiring

Finally, Gratatouille encourages exposing extensions to users instead of task classes directly. All generated code is generated as `internal`. This makes it easier to have some inputs user configurable while some others are an implementation details and more generally makes it easier to evolve the public API of your plugin.

When a task has a high number of inputs, it can become hard to track which ones have been wired and which ones haven't. By using a central registration point, Gratatouille enforces at build time that all inputs/outputs have been properly wired.

