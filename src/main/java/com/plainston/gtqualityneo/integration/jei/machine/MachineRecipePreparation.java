package com.plainston.gtqualityneo.integration.jei.machine;

import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import java.util.ArrayList;
import java.util.List;

/** Prepares the visible machine recipes once per recipe browser reload. */
public final class MachineRecipePreparation {
    private MachineRecipePreparation() {}

    public static List<Recipe> prepare(RecipeMap map, RecipeVariants.Mode mode) throws ReflectiveOperationException {
        synchronized (map) {
            List<Recipe> recipes;
            try (var saves = RecipeConfigBatch.begin(); var checks = RecipeRegistrationFilter.begin(map)) {
                // GT6 removes completed handlers while iterating; a snapshot avoids skipping their neighbours.
                for (var handler : new ArrayList<>(map.mRecipeMapHandlers)) {
                    handler.addAllRecipes(map);
                    if (handler.isDone()) map.mRecipeMapHandlers.remove(handler);
                }
                // Hammer/crucible maps append virtual recipes outside mRecipeList.
                recipes = map.getClass().getMethod("getNEIAllRecipes").getDeclaringClass() != RecipeMap.class
                    ? map.getNEIAllRecipes() : new ArrayList<>(map.mRecipeList);
            }
            var visible = recipes.stream().filter(recipe -> recipe.mEnabled && !recipe.mHidden).toList();
            return mode.any() ? RecipeVariants.group(visible, mode) : visible;
        }
    }
}
