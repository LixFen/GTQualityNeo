// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新配方字段或 JEI API 后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.machine;

import com.plainston.gtqualityneo.integration.jei.compat.DynamicRecipeIngredients;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** GTQualityNeo owns the page; GT6 remains the source of recipes and machine semantics. */
public final class MachineRecipeCategory extends AbstractRecipeCategory<Recipe> implements DynamicRecipeIngredients.Category {
    private final RecipeMap map;

    public MachineRecipeCategory(RecipeMap map, IRecipeType<Recipe> type, IGuiHelper helper) {
        super(type, Component.translatableWithFallback(map.mNameInternal, map.mNameLocal),
            map.mRecipeMachineList.isEmpty() ? helper.createBlankDrawable(16, 16)
                : helper.createDrawableItemStack(map.mRecipeMachineList.getFirst()),
            RecipeLayout.WIDTH, MachineRecipeDisplay.layout(map).height());
        this.map = map;
    }

    @Override public RecipeMap gtqualityneo$recipeMap() { return map; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        MachineRecipeDisplay.setRecipe(map, builder, recipe);
    }
    @Override public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        MachineRecipeDisplay.draw(map, recipe, graphics);
    }
    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        MachineRecipeDisplay.extras(map, recipe, builder);
    }
}
