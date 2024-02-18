package recipes

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

public abstract class RecipesExtension {
    abstract val persons: Property<Int>
    abstract val recipe: RegularFileProperty
}

class RecipesPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("recipes", RecipesExtension::class.java)

        val prepareIngredients = project.registerPrepareIngredientsTask(
            persons = extension.persons
        )

        project.registerCookTask(
            recipe = extension.recipe,
            ingredients = prepareIngredients.flatMap { it.outputFile }
        )
    }
}