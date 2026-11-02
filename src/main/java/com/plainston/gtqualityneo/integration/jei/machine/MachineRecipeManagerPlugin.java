// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新动态配方查询规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.machine;

import com.mojang.logging.LogUtils;
import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.data.FL;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.advanced.IRecipeManagerPluginHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** One JEI plugin dispatches to the existing per-map caches without generating recipes at registration. */
public final class MachineRecipeManagerPlugin implements IRecipeManagerPlugin {
    private final Map<IRecipeType<?>, Lookup> lookups = new LinkedHashMap<>();
    private final List<IRecipeType<?>> recipeTypes;
    private final IRecipeManagerPluginHelper helper;

    public MachineRecipeManagerPlugin(Map<RecipeMap, IRecipeType<Recipe>> types, IRecipeManagerPluginHelper helper) {
        this.helper = helper;
        types.forEach((map, type) -> {
            try { lookups.put(type, new Lookup(map)); }
            catch (RuntimeException e) { LogUtils.getLogger().error("Skipping JEI lookup for RecipeMap '{}'", map.mNameInternal, e); }
        });
        recipeTypes = List.copyOf(lookups.keySet());
    }

    private static boolean supported(ITypedIngredient<?> ingredient) {
        return ingredient != null && (ingredient.getIngredient() instanceof FluidStack
            || ingredient.getItemStack().filter(stack -> !stack.isEmpty()).isPresent());
    }

    @Override public <V> List<IRecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        if ((focus.getRole() == RecipeIngredientRole.INPUT || focus.getRole() == RecipeIngredientRole.OUTPUT)
            && supported(focus.getTypedValue())) return recipeTypes;
        var result = new ArrayList<IRecipeType<?>>();
        for (var type : recipeTypes) if (helper.isCraftingStation(type, focus)) result.add(type);
        return result;
    }

    @Override public <T, V> List<T> getRecipes(IRecipeType<T> type, IFocus<V> focus) {
        var lookup = lookups.get(type);
        if (lookup == null) return List.of();
        boolean station = helper.isCraftingStation(type, focus);
        List<Recipe> recipes = switch (focus.getRole()) {
            case INPUT -> lookup.find(focus.getTypedValue(), true);
            case OUTPUT -> lookup.find(focus.getTypedValue(), false);
            default -> List.of();
        };
        if (station) {
            if (focus.getRole() == RecipeIngredientRole.CRAFTING_STATION) return cast(lookup.getAllRecipes());
            if (focus.getRole() == RecipeIngredientRole.INPUT || focus.getRole() == RecipeIngredientRole.OUTPUT) {
                // Iterate the resolved pages, not lazyAll's early display copy, so overlaps deduplicate.
                var combined = new LinkedHashSet<>(recipes);
                combined.addAll(lookup.getAllRecipes());
                return cast(List.copyOf(combined));
            }
        }
        return cast(recipes);
    }

    @Override public <T> List<T> getRecipes(IRecipeType<T> type) {
        var lookup = lookups.get(type);
        return lookup == null ? List.of() : cast(lookup.getAllRecipes());
    }

    // Only IRecipeType<Recipe> keys enter the map above.
    @SuppressWarnings("unchecked")
    private static <T> List<T> cast(List<Recipe> recipes) { return (List<T>) recipes; }

    private static final class Lookup {
        private final RecipeMap map;
        private final MachineRecipeLookup lookup;

        Lookup(RecipeMap map) {
            this.map = map;
            lookup = new MachineRecipeLookup(map, () -> new RecipeVariants.Mode(
                GTQualityNeo.JEI_MACHINE_LOOKUP.get() && GTQualityNeo.JEI_FLUID_VARIANTS.get(),
                GTQualityNeo.JEI_MACHINE_LOOKUP.get() && GTQualityNeo.JEI_OREDICT_VARIANTS.get()));
        }

        private List<Recipe> find(ITypedIngredient<?> ingredient, boolean input) {
            if (!supported(ingredient)) return List.of();
            ItemStack stack = ingredient.getIngredient() instanceof FluidStack fluid
                ? FL.display(fluid, false, false) : ingredient.getItemStack().orElseThrow();
            if (GTQualityNeo.JEI_MACHINE_LOOKUP.get()) return lookup.find(stack, input);
            try { return input ? map.getNEIUsagesFor(stack) : map.getNEIRecipesFor(stack); }
            catch (RuntimeException e) { e.printStackTrace(gregapi.data.CS.ERR); return List.of(); }
        }

        List<Recipe> getAllRecipes() { return lookup.lazyAll(); }
    }
}
