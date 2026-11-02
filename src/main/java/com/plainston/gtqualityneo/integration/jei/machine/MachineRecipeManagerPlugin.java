// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新动态配方查询规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.machine;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.data.FL;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;

public final class MachineRecipeManagerPlugin implements ISimpleRecipeManagerPlugin<Recipe> {
    private final RecipeMap map;
    private final MachineRecipeLookup lookup;

    public MachineRecipeManagerPlugin(RecipeMap map) {
        this.map = map;
        lookup = new MachineRecipeLookup(map, () -> new RecipeVariants.Mode(
            GTQualityNeo.JEI_MACHINE_LOOKUP.get() && GTQualityNeo.JEI_FLUID_VARIANTS.get(),
            GTQualityNeo.JEI_MACHINE_LOOKUP.get() && GTQualityNeo.JEI_OREDICT_VARIANTS.get()));
    }

    private static boolean supported(ITypedIngredient<?> ingredient) {
        return ingredient != null && (ingredient.getIngredient() instanceof FluidStack
            || ingredient.getItemStack().filter(stack -> !stack.isEmpty()).isPresent());
    }
    private List<Recipe> find(ITypedIngredient<?> ingredient, boolean input) {
        if (!supported(ingredient)) return List.of();
        ItemStack stack = ingredient.getIngredient() instanceof FluidStack fluid
            ? FL.display(fluid, false, false) : ingredient.getItemStack().orElseThrow();
        if (GTQualityNeo.JEI_MACHINE_LOOKUP.get()) return lookup.find(stack, input);
        try { return input ? map.getNEIUsagesFor(stack) : map.getNEIRecipesFor(stack); }
        catch (RuntimeException e) { e.printStackTrace(gregapi.data.CS.ERR); return List.of(); }
    }

    @Override public boolean isHandledInput(ITypedIngredient<?> input) { return supported(input); }
    @Override public boolean isHandledOutput(ITypedIngredient<?> output) { return supported(output); }
    @Override public List<Recipe> getRecipesForInput(ITypedIngredient<?> input) { return find(input, true); }
    @Override public List<Recipe> getRecipesForOutput(ITypedIngredient<?> output) { return find(output, false); }
    @Override public List<Recipe> getAllRecipes() { return lookup.lazyAll(); }
}
