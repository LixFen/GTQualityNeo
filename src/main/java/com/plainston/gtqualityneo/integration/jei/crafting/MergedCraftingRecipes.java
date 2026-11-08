// Compatibility: GT6w alpha.18 owns the serializable crafting displays and their material variants.
package com.plainston.gtqualityneo.integration.jei.crafting;

import com.plainston.gtqualityneo.integration.RecipeMaterialVisibility;
import gregapi.jei.GT6_JEI_CraftingRecipes;
import gregapi.recipes.ICraftingRecipeGT;
import gregapi.util.CR;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.ArrayList;
import java.util.List;

/** Registers GT6's native crafting displays once so JEI can index them before the first query. */
public final class MergedCraftingRecipes {
    private MergedCraftingRecipes() {}

    public static void register(IRecipeRegistration registration) {
        registration.addRecipes(RecipeTypes.CRAFTING, prepare(CR.list()));
    }

    static List<RecipeHolder<CraftingRecipe>> prepare(List<ICraftingRecipeGT> recipes) {
        var holders = new ArrayList<RecipeHolder<CraftingRecipe>>();
        int index = 0;
        for (var recipe : recipes) {
            if (!GT6_JEI_CraftingRecipes.showable(recipe)) continue;
            for (var adapter : GT6_JEI_CraftingRecipes.adapters(recipe, index)) {
                if (RecipeMaterialVisibility.hidden(adapter.output())) continue;
                var holder = adapter.inputs().stream().anyMatch(RecipeMaterialVisibility::hidden)
                    ? VisibleCraftingRecipe.filter(adapter.holder()) : adapter.holder();
                if (holder != null) holders.add(holder);
            }
            index++;
        }
        return List.copyOf(holders);
    }
}
