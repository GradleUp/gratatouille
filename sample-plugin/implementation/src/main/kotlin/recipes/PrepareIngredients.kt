package recipes

import gratatouille.GTaskAction
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
internal data class Ingredients(
    val tomatoes: Int,
    val zucchinis: Int,
    val eggplants: Int,
    val bellPeppers: Int,
    val onions: Int,
    val garlic: Float,
    val salt: Float,
    val pepper: Float,
)

@GTaskAction
internal fun prepareIngredients(persons: Int): Ingredients {
    return Ingredients(
        tomatoes = (persons * 0.75).roundToInt(),
        zucchinis = (persons * 0.3).roundToInt(),
        eggplants = (persons * 0.3).roundToInt(),
        bellPeppers = (persons * 0.3).roundToInt(),
        onions = (persons * 0.5).roundToInt(),
        garlic = persons * 5f,
        salt = persons * 1f,
        pepper = persons * 1f
    )
}