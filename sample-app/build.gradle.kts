plugins {
    id("recipes")
}

recipes {
    persons.set(2)
    recipe.set(file("recipe.txt"))
}