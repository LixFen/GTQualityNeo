package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.DynamicRecipeIngredients;
import mezz.jei.library.ingredients.RecipeIngredientSupplier.FocusLink;
import mezz.jei.library.recipes.collect.RecipeTypeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Pseudo
@Mixin(targets = "mezz.jei.library.recipes.RecipeManagerInternal", remap = false)
public abstract class JeiDynamicIngredientsExtractMixin {
    // The second return follows extraction. The first return is the existing cache hit path.
    @Inject(method = "getFocusLinks", at = @At(value = "RETURN", ordinal = 1))
    private <T> void gtqualityneo$rememberIngredients(RecipeTypeData<T> data, T recipe, CallbackInfoReturnable<List<FocusLink>> cir) {
        ((DynamicRecipeIngredients.Cache) data).gtqualityneo$cacheIngredients(recipe, cir.getReturnValue());
    }
}
