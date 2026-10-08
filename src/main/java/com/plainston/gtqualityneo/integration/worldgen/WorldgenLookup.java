package com.plainston.gtqualityneo.integration.worldgen;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;

public final class WorldgenLookup implements ISimpleRecipeManagerPlugin<WorldgenCatalog.Page> {
    private List<WorldgenCatalog.Page> pages;
    private synchronized List<WorldgenCatalog.Page> pages() {
        // JEI can probe this category before GT6 has completed its deferred worldgen registration.
        if (pages == null || pages.isEmpty()) pages = WorldgenCatalog.collect();
        return pages;
    }
    private List<WorldgenCatalog.Page> find(ITypedIngredient<?> ingredient, boolean usage) {
        Object value = ingredient.getIngredient();
        if (value instanceof ItemStack stack) return pages().stream().filter(page -> page.matches(stack, usage)).toList();
        if (value instanceof FluidStack fluid) return pages().stream().filter(page -> page.fluid == fluid.getFluid()).toList();
        return List.of();
    }
    @Override public boolean isHandledInput(ITypedIngredient<?> input) { return !find(input, true).isEmpty(); }
    @Override public boolean isHandledOutput(ITypedIngredient<?> output) { return !find(output, false).isEmpty(); }
    @Override public List<WorldgenCatalog.Page> getRecipesForInput(ITypedIngredient<?> input) { return find(input, true); }
    @Override public List<WorldgenCatalog.Page> getRecipesForOutput(ITypedIngredient<?> output) { return find(output, false); }
    @Override public List<WorldgenCatalog.Page> getAllRecipes() { return pages(); }
}
