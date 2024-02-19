# Sample plugin

Sample plugin that:
* prepares ingredients given a number of guests.
* cooks the ingredients into a nice ratatouille.

The tasks actions are in [implementation/src/main/kotlin/recipes/](implementation/src/main/kotlin/recipes/). 

The wiring logic is in [gradle-plugin/src/main/kotlin/recipes/RecipesPlugin.kt](gradle-plugin/src/main/kotlin/recipes/RecipesPlugin.kt).

Generated sources are generated in `implementation/build/generated/ksp/main`. They are also extracted in `gradle-plugin/build/extractGratatouilleSources`. 

