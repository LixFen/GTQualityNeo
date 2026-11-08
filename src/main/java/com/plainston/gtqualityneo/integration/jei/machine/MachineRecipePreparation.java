package com.plainston.gtqualityneo.integration.jei.machine;

import com.plainston.gtqualityneo.integration.RecipeMaterialVisibility;
import gregapi.data.OP;
import gregapi.data.RM;
import gregapi.oredict.OreDictMaterial;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.recipes.handlers.RecipeMapHandlerCrushing;
import gregapi.recipes.handlers.RecipeMapHandlerPrefix;
import gregapi.recipes.handlers.RecipeMapHandlerPrefixShredding;
import gregapi.util.OM;
import gregapi.util.ST;
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
                    // These maps deliberately disable the prefix handler's bulk hook in GT6.
                    // Finish only their finite prefix rules before building the browser's snapshot.
                    if (handler instanceof RecipeMapHandlerPrefix prefix
                        && (map == RM.Mortar || map == RM.Compressor || map == RM.Cutter
                            || map == RM.Shredder && prefix instanceof RecipeMapHandlerPrefixShredding))
                        prefix.addAllRecipesInternal(map);
                    else handler.addAllRecipes(map);
                    // Crushing only implements focused queries; its bulk hook generates nothing.
                    // Use GT6's output query to populate canonical ore inputs before indexing them.
                    if (handler instanceof RecipeMapHandlerCrushing crushing) {
                        for (var material : OreDictMaterial.MATERIAL_ARRAY) {
                            if (material == null || material.mTargetedCrushing.isEmpty()) continue;
                            var output = OP.crushed.mat(material, 1);
                            if (!ST.valid(output)) output = OP.dust.mat(material, 1);
                            if (!ST.valid(output)) output = OP.gem.mat(material, 1);
                            if (ST.valid(output)) crushing.addRecipesProducing(map, true, output,
                                OM.data_(output));
                        }
                    }
                    if (handler.isDone()) map.mRecipeMapHandlers.remove(handler);
                }
                // Hammer/crucible maps append virtual recipes outside mRecipeList.
                recipes = map.getClass().getMethod("getNEIAllRecipes").getDeclaringClass() != RecipeMap.class
                    ? map.getNEIAllRecipes() : new ArrayList<>(map.mRecipeList);
            }
            var visible = recipes.stream().filter(RecipeMaterialVisibility::visible).toList();
            return mode.any() ? RecipeVariants.group(visible, mode) : visible;
        }
    }
}
