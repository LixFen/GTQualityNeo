package com.plainston.gtqualityneo.integration.worldgen;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponentPatch;

public final class WorldgenLookup implements ISimpleRecipeManagerPlugin<WorldgenCatalog.Page> {
    private List<WorldgenCatalog.Page> pages;
    private final Map<Focus, List<WorldgenCatalog.Page>> focused = new LinkedHashMap<>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Focus, List<WorldgenCatalog.Page>> entry) { return size() > 64; }
    };
    private record Focus(Object type, int count, DataComponentPatch components, boolean usage) {}
    private synchronized List<WorldgenCatalog.Page> pages() {
        // JEI can probe this category before GT6 has completed its deferred worldgen registration.
        if (pages == null || pages.isEmpty()) { pages = WorldgenCatalog.collect(); focused.clear(); }
        return pages;
    }
    private synchronized List<WorldgenCatalog.Page> find(ITypedIngredient<?> ingredient, boolean usage) {
        Object value = ingredient.getIngredient();
        Focus focus;
        if (value instanceof ItemStack stack) focus = new Focus(stack.getItem(), stack.getCount(), stack.getComponentsPatch(), usage);
        else if (value instanceof FluidStack fluid) focus = new Focus(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch(), usage);
        else return List.of();
        var catalog = pages();
        if (catalog.isEmpty()) return List.of();
        return focused.computeIfAbsent(focus, ignored -> {
            if (value instanceof ItemStack stack) {
                var query = new WorldgenCatalog.Query(stack);
                return catalog.stream().filter(page -> page.matches(query, usage)).toList();
            }
            var fluid = (FluidStack) value;
            return catalog.stream().filter(page -> page.fluid == fluid.getFluid()).toList();
        });
    }
    @Override public boolean isHandledInput(ITypedIngredient<?> input) { return !find(input, true).isEmpty(); }
    @Override public boolean isHandledOutput(ITypedIngredient<?> output) { return !find(output, false).isEmpty(); }
    @Override public List<WorldgenCatalog.Page> getRecipesForInput(ITypedIngredient<?> input) { return find(input, true); }
    @Override public List<WorldgenCatalog.Page> getRecipesForOutput(ITypedIngredient<?> output) { return find(output, false); }
    @Override public List<WorldgenCatalog.Page> getAllRecipes() { return pages(); }
}
