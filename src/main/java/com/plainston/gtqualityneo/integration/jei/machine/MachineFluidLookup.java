package com.plainston.gtqualityneo.integration.jei.machine;

import gregapi.data.FL;
import gregapi.item.ItemFluidDisplay;
import gregapi.recipes.Recipe;
import gregapi.util.ST;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Bridges GT6 fluid icons to the prepared cards, independently of their display amount/NBT. */
final class MachineFluidLookup implements ISimpleRecipeManagerPlugin<Recipe> {
    private final Map<Fluid, List<Recipe>> inputs = new IdentityHashMap<>();
    private final Map<Fluid, List<Recipe>> outputs = new IdentityHashMap<>();

    void index(List<Recipe> recipes) {
        inputs.clear();
        outputs.clear();
        for (var recipe : recipes) {
            for (int slot = 0; slot < recipe.mFluidInputs.length; slot++)
                for (var fluid : RecipeVariants.inputOptions(recipe, slot)) add(inputs, fluid, recipe);
            for (var fluid : recipe.mFluidOutputs) add(outputs, fluid, recipe);
        }
    }

    private static void add(Map<Fluid, List<Recipe>> index, FluidStack fluid, Recipe recipe) {
        if (fluid == null || fluid.isEmpty()) return;
        var matches = index.computeIfAbsent(fluid.getFluid(), ignored -> new ArrayList<>());
        if (matches.isEmpty() || matches.getLast() != recipe) matches.add(recipe);
    }

    private static List<Recipe> find(Map<Fluid, List<Recipe>> index, ITypedIngredient<?> ingredient) {
        if (!(ingredient.getIngredient() instanceof ItemStack stack)
            || !(stack.getItem() instanceof ItemFluidDisplay)) return List.of();
        return index.getOrDefault(FL.fluid(ST.meta_(stack)), List.of());
    }

    @Override public boolean isHandledInput(ITypedIngredient<?> input) { return !find(inputs, input).isEmpty(); }
    @Override public boolean isHandledOutput(ITypedIngredient<?> output) { return !find(outputs, output).isEmpty(); }
    @Override public List<Recipe> getRecipesForInput(ITypedIngredient<?> input) { return find(inputs, input); }
    @Override public List<Recipe> getRecipesForOutput(ITypedIngredient<?> output) { return find(outputs, output); }
    // These same cards are already registered in JEI's native index.
    @Override public List<Recipe> getAllRecipes() { return List.of(); }
}
