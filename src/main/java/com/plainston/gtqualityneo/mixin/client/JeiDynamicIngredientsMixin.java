package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.DynamicRecipeIngredients;
import com.plainston.gtqualityneo.integration.jei.RecipeIngredientCache;
import com.plainston.gtqualityneo.integration.jei.RecipeMapRevision;
import gregapi.recipes.Recipe;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.library.ingredients.RecipeIngredientSupplier.FocusLink;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Pseudo
@Mixin(targets = "mezz.jei.library.recipes.collect.RecipeTypeData", remap = false)
public abstract class JeiDynamicIngredientsMixin<T> implements DynamicRecipeIngredients.Cache {
    @Shadow @Final private IRecipeCategory<T> recipeCategory;
    @Unique private final RecipeIngredientCache<Recipe, List<FocusLink>> gtqualityneo$ingredients = new RecipeIngredientCache<>();

    @Inject(method = "getFocusLinks", at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$reuseIngredients(T recipe, CallbackInfoReturnable<List<FocusLink>> cir) {
        if (!(recipe instanceof Recipe gtRecipe) || !(recipeCategory instanceof DynamicRecipeIngredients.Category owner)) return;
        long version = ((RecipeMapRevision) owner.gtqualityneo$recipeMap()).gtqualityneo$recipeRevision();
        var cached = gtqualityneo$ingredients.get(gtRecipe, version, DynamicRecipeIngredients.resourceVersion());
        if (cached != null) cir.setReturnValue(cached);
    }

    @Override public void gtqualityneo$cacheIngredients(Object recipe, List<?> links) {
        if (!(recipe instanceof Recipe gtRecipe) || !(recipeCategory instanceof DynamicRecipeIngredients.Category owner)) return;
        long version = ((RecipeMapRevision) owner.gtqualityneo$recipeMap()).gtqualityneo$recipeRevision();
        @SuppressWarnings("unchecked") var typed = (List<FocusLink>) links;
        gtqualityneo$ingredients.put(gtRecipe, List.copyOf(typed), version, DynamicRecipeIngredients.resourceVersion());
    }
}
