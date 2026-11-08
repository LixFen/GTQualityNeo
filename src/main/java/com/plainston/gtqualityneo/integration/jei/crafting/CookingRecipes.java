package com.plainston.gtqualityneo.integration.jei.crafting;

import com.mojang.logging.LogUtils;
import com.plainston.gtqualityneo.integration.RecipeMaterialVisibility;
import gregapi.recipes.FurnaceRecipes;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/** Register GT6's existing cooking views once instead of rescanning furnace snapshots for every lookup. */
public final class CookingRecipes {
    private static final List<Consumer<IRecipeRegistration>> pending = new ArrayList<>();
    private CookingRecipes() {}

    public static void reset() { pending.clear(); }

    public static <T> boolean defer(IRecipeType<T> type, ISimpleRecipeManagerPlugin<T> plugin) {
        if (!type.equals(RecipeTypes.SMOKING) && !type.equals(RecipeTypes.BLASTING)) return false;
        pending.add(registration -> {
            try { registration.addRecipes(type, plugin.getAllRecipes().stream().map(CookingRecipes::visible).filter(Objects::nonNull).toList()); }
            catch (RuntimeException e) { LogUtils.getLogger().error("Could not register GT6 cooking recipes for {}", type.getUid(), e); }
        });
        return true;
    }

    public static void register(IRecipeRegistration registration) {
        try { for (var task : pending) task.accept(registration); }
        finally { reset(); }
    }

    @SuppressWarnings("unchecked")
    static <T> T visible(T value) {
        if (!(value instanceof RecipeHolder<?> holder) || !(holder.value() instanceof AbstractCookingRecipe recipe)) return value;
        var display = recipe.display().getFirst();
        var output = ((SlotDisplay.ItemStackSlotDisplay) display.result()).stack();
        if (RecipeMaterialVisibility.hidden(output.create())) return null;
        var inputs = VisibleCraftingRecipe.stacks(recipe.input().display());
        var visible = inputs.stream().filter(stack -> !RecipeMaterialVisibility.hidden(stack)).toList();
        if (inputs.size() == visible.size()) return value;
        if (visible.isEmpty()) return null;
        var ingredient = new FurnaceRecipes.RuntimeIngredient(visible).toVanilla();
        var info = new Recipe.CommonInfo(recipe.showNotification());
        var book = new AbstractCookingRecipe.CookingBookInfo(recipe.category(), recipe.group());
        AbstractCookingRecipe filtered = recipe instanceof SmokingRecipe
            ? new SmokingRecipe(info, book, ingredient, output, recipe.experience(), recipe.cookingTime())
            : new BlastingRecipe(info, book, ingredient, output, recipe.experience(), recipe.cookingTime());
        return (T) new RecipeHolder<>(holder.id(), filtered);
    }
}
