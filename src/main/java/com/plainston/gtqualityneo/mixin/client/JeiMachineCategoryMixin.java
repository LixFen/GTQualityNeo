package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.MachineRecipeDisplay;
import com.plainston.gtqualityneo.integration.jei.DynamicRecipeIngredients;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "gregapi.jei.GT6_JEI_RecipeCategory", remap = false)
public abstract class JeiMachineCategoryMixin implements DynamicRecipeIngredients.Category {
    @Shadow @Final private RecipeMap mMap;

    @Override public RecipeMap gtqualityneo$recipeMap() { return mMap; }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 161, ordinal = 0))
    private static int gtqualityneo$height(int original, RecipeMap map, RecipeType<Recipe> type, IGuiHelper helper) {
        return MachineRecipeDisplay.layout(map).height();
    }

    @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lgregapi/recipes/Recipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$slots(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses, CallbackInfo ci) {
        MachineRecipeDisplay.setRecipe(mMap, builder, recipe);
        ci.cancel();
    }

    @Inject(method = "draw(Lgregapi/recipes/Recipe;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphicsExtractor;DD)V", at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics,
                                  double mouseX, double mouseY, CallbackInfo ci) {
        MachineRecipeDisplay.draw(mMap, recipe, graphics);
        ci.cancel();
    }

    @Inject(method = "createRecipeExtras(Lmezz/jei/api/gui/widgets/IRecipeExtrasBuilder;Lgregapi/recipes/Recipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$extras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses, CallbackInfo ci) {
        MachineRecipeDisplay.extras(mMap, recipe, builder);
        ci.cancel();
    }
}
