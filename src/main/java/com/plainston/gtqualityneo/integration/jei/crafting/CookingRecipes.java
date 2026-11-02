package com.plainston.gtqualityneo.integration.jei.crafting;

import com.mojang.logging.LogUtils;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Register GT6's existing cooking views once instead of rescanning furnace snapshots for every lookup. */
public final class CookingRecipes {
    private static final List<Consumer<IRecipeRegistration>> pending = new ArrayList<>();
    private CookingRecipes() {}

    public static void reset() { pending.clear(); }

    public static <T> boolean defer(IRecipeType<T> type, ISimpleRecipeManagerPlugin<T> plugin) {
        if (!type.equals(RecipeTypes.SMOKING) && !type.equals(RecipeTypes.BLASTING)) return false;
        pending.add(registration -> {
            try { registration.addRecipes(type, plugin.getAllRecipes()); }
            catch (RuntimeException e) { LogUtils.getLogger().error("Could not register GT6 cooking recipes for {}", type.getUid(), e); }
        });
        return true;
    }

    public static void register(IRecipeRegistration registration) {
        try { for (var task : pending) task.accept(registration); }
        finally { reset(); }
    }
}
