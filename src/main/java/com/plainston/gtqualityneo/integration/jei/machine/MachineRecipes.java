// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、类别 ID 或查看配方入口后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.machine;

import com.mojang.logging.LogUtils;
import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.data.MD;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One registry per JEI lifecycle. Stable GT6 category IDs retain existing bookmarks and GUI links.
 * Each map is isolated as in GT6's own plugin: a failing map is skipped, the others still register.
 */
public final class MachineRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MachineRecipes active;
    private final Map<RecipeMap, IRecipeType<Recipe>> types = new LinkedHashMap<>();
    private final Map<String, IRecipeType<Recipe>> names = new LinkedHashMap<>();
    private final Map<RecipeMap, MachineFluidLookup> fluids = new LinkedHashMap<>();
    private IJeiRuntime runtime;

    void rebuild(Collection<RecipeMap> maps) {
        types.clear(); names.clear(); fluids.clear();
        for (var map : maps) {
            if (!map.mNEIAllowed) continue;
            boolean taken = names.containsKey(map.mNameNEI);
            var type = IRecipeType.create(MD.GT.mID, taken ? map.mNameInternal : map.mNameNEI, Recipe.class);
            types.put(map, type);
            fluids.put(map, new MachineFluidLookup());
            if (!taken) names.put(map.mNameNEI, type);
        }
    }

    public void registerCategories(IRecipeCategoryRegistration registration) {
        rebuild(RecipeMap.RECIPE_MAP_LIST);
        active = this;
        var helper = registration.getJeiHelpers().getGuiHelper();
        for (var iterator = types.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            try {
                registration.addRecipeCategories(new MachineRecipeCategory(entry.getKey(), entry.getValue(), helper));
            } catch (RuntimeException e) {
                LOGGER.error("Skipping JEI category for RecipeMap '{}'", entry.getKey().mNameInternal, e);
                names.values().remove(entry.getValue());
                iterator.remove();
            }
        }
    }
    public void registerRecipes(IRecipeRegistration registration) {
        var mode = new RecipeVariants.Mode(GTQualityNeo.JEI_FLUID_VARIANTS.get(), GTQualityNeo.JEI_OREDICT_VARIANTS.get());
        types.forEach((map, type) -> {
            try {
                var recipes = MachineRecipePreparation.prepare(map, mode);
                registration.addRecipes(type, recipes);
                fluids.get(map).index(recipes);
            }
            catch (Exception e) { LOGGER.error("Skipping JEI recipes for RecipeMap '{}'", map.mNameInternal, e); }
        });
    }

    public void registerAdvanced(IAdvancedRegistration registration) {
        types.forEach((map, type) -> registration.addSimpleRecipeManagerPlugin(type, fluids.get(map)));
    }

    public void registerCatalysts(IRecipeCatalystRegistration registration) {
        types.forEach((map, type) -> {
            try {
                if (!map.mRecipeMachineList.isEmpty()) registration.addCraftingStation(type, map.mRecipeMachineList.toArray(ItemStack[]::new));
            } catch (RuntimeException e) { LOGGER.error("Skipping JEI catalysts for RecipeMap '{}'", map.mNameInternal, e); }
        });
    }
    public void runtime(IJeiRuntime runtime) { this.runtime = runtime; }
    public static boolean show(String name) {
        var registry = active;
        if (registry == null || registry.runtime == null || name == null) return false;
        var type = registry.names.get(name);
        if (type == null) return false;
        registry.runtime.getRecipesGui().showTypes(List.of(type));
        return true;
    }
}
