package com.plainston.gtqualityneo.integration.multiblock;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class MultiblockLookup implements ISimpleRecipeManagerPlugin<MultiblockCatalog.Page> {
    private List<MultiblockCatalog.Page> pages = List.of();
    private net.minecraft.core.RegistryAccess registries;
    private List<MultiblockCatalog.Page> pages() {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        if (registries != level.registryAccess()) {
            registries = level.registryAccess();
            pages = MultiblockCatalog.collect(registries);
        }
        return pages;
    }
    private List<MultiblockCatalog.Page> find(ITypedIngredient<?> ingredient, boolean usage) {
        if (!(ingredient.getIngredient() instanceof ItemStack stack)) return List.of();
        return pages().stream().filter(page -> ItemStack.isSameItemSameComponents(stack, page.controllerStack())
            || usage && page.ingredients.stream().anyMatch(part -> ItemStack.isSameItemSameComponents(stack, part))).toList();
    }
    @Override public boolean isHandledInput(ITypedIngredient<?> input) { return !find(input, true).isEmpty(); }
    @Override public boolean isHandledOutput(ITypedIngredient<?> output) { return !find(output, false).isEmpty(); }
    @Override public List<MultiblockCatalog.Page> getRecipesForInput(ITypedIngredient<?> input) { return find(input, true); }
    @Override public List<MultiblockCatalog.Page> getRecipesForOutput(ITypedIngredient<?> output) { return find(output, false); }
    @Override public List<MultiblockCatalog.Page> getAllRecipes() { return pages(); }
}
