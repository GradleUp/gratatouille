# Gratatouille 🐘🤝🐭👉👨‍🍳 

Gratatouille is an opinionated framework to build Gradle plugins. Write pure Kotlin functions and the Gratatouille KSP processor generates tasks, workers and wiring code for you.

Gratatouille enforces a clear separation between your plugin logic (**implementation**) and your plugin wiring (**gradle-plugin**) making your plugin immune to classloader issues 🛡️. 

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

# Quick Start

## Step 1/2: `com.gradleup.gratatouille.implementation` 

Create an `implementation` module for your plugin implementation and apply the `com.gradleup.gratatouille.implementation` plugin:

```kotlin
// implementation/build.gradle.kts
plugins {
    id("com.gradleup.gratatouille.implementation")
}

dependencies {
    // Add the gratatouille annotations
    implementation("com.gradleup.gratatouille:gratatouille-core")
    // Add other dependencies
    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation("org.ow2.asm:asm-commons:9.6")
    // do **not** add gradleApi() here
}
```

Write your plugin task action as a top-level Kotlin pure function annotated with `@GTaskAction`:

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

Gratatouille automatically maps function parameters to Gradle inputs and the return value to a Gradle output.

Gratatouille generates entry points, task, workers and Gradle wiring code that can be used from your plugin.

## Step 2/2 `com.gradleup.gratatouille.plugin` 

To use the generated code in your plugin, create a `gradle-plugin` module next to your `implementation` module. 

By using two different modules, Gratatouille ensures that Gradle classes do not leak in your plugin implementation and vice-versa.

The `gradle-plugin` module should depend on the Gradle API and apply the `com.gradleup.gratatouille.plugin`:

```kotlin
// implementation/build.gradle.kts
plugins {
    id("com.gradleup.gratatouille.plugin")
}

dependencies {
    // Add your implementation module to the "gratatouille" configuration.
    // This adds the wiring code to the main source set. No dependency is pulled 
    // in the plugin classpath.
    gratatouille(project(":implementation"))
    // Add the gradle API 
    implementation(gradleApi())
}

// Create your plugin as usual, see https://docs.gradle.org/current/userguide/java_gradle_plugin.html 
gradlePlugin {
    // ... 
}
```

In your plugin code, use `Project.registerPrepareIngredientsTask()` to register the task:

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

No need to deal with stateful task properties and Gradle annotations. Your plugin code is a side-effect free function. 

## Built-in kotlinx.serialization support

Gratatouille has builtin support for [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization). You don't need to deal with serialization, the models are serialized and deserialized as needed.

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

Gratatouille allocate paths for output files and directory automatically. Each output gets a dedicated filesystem location at `"build/gtask/${taskName}/${outputName}"`. 

This way:
* you don't have to set inputs.
* issues like [#26091](https://github.com/gradle/gradle/issues/26091) are avoided by construction.

If your function has a single return value, Gratatouille uses `outputFile` as output name.

If your function needs multiple return values, wrap them in a non-serializable class.

If you need to control the output location of an output, you can do so using `@GManuallyWired`.

In your implementation:

```kotlin
@GTaskAction
internal fun cook(
    recipe: GInputFile,
    ingredients: Ingredients,
    // outputFile will be exposed in registerCookTask(outputFile) so you can configure it 
    @GManuallyWired outputFile: GOutputFile
) {
    outputFile.writeText(// cook here!)
}
```

In your plugin:

```kotlin
project.registerCookTask(
    recipe = extension.recipe,
    ingredients = prepareIngredients.flatMap { it.outputFile },
    // Set outputFile location explicitly
    outputFile = project.layout.buildDirectory.file("outputFile")
)
```

## Classloader isolation by default

Gratatouille creates a separate classloader for each task and calls your pure function using reflection. 

This means your plugin can depend on popular dependencies such as the Kotlin stdlib, KotlinPoet or ASM without risking conflicts with other plugins or the Gradle classpath itself. 

## Build cache by default

`@CacheableTask` is enabled by default. All input files use `PathSensitivity.RELATIVE` by default making your tasks easily relocatable.

## Easy documentation

`@GTaskAction` takes a `taskDescription` and a `taskGroup` argument making it easy to document your tasks:

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

Because your task actions are pure Kotlin function, no state is shared, making them perfect candidates for parallelisation. 

Gratatouille uses the [Worker API](https://docs.gradle.org/current/userguide/worker_api.html) to allow parallel execution making you build faster in general. Use `org.gradle.workers.max` to control the maximum number of workers.

## Compile time task wiring

Finally, Gratatouille encourages exposing extensions to users instead of task classes directly. This makes it easier to have some inputs user configurable while some others are an implementation details and more generally makes it easier to evolve the public API of your plugin.

When a task has a high number of inputs, it can become hard to track which ones have been wired and which ones haven't. By using a central registration point, Gratatouille enforces at build time that all inputs/outputs have been properly wired.


# Sample plugin

* Open [sample-plugin](sample-plugin) in IntelliJ for an example plugin.
* Open [sample-app](sample-app) in IntelliJ and run `./gradlew -p sample-app cook` for an example app using the example plugin.