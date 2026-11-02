// 兼容说明：依赖 GT6w alpha.17 / 当前 JEI 的接口；未来上游更新后本适配可能过时，需复核。
package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.compat.DynamicRecipeIngredients;
import com.plainston.gtqualityneo.integration.jei.cache.RecipeIngredientCache;
import com.plainston.gtqualityneo.integration.jei.compat.RecipeMapRevision;
import com.plainston.gtqualityneo.integration.jei.crafting.MergedCraftingRecipes;
import com.plainston.gtqualityneo.integration.jei.machine.RecipeVariants;
import gregapi.recipes.Recipe;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.library.ingredients.RecipeIngredientSupplier.FocusLink;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

/**
 * JEI precomputes focus links only for registered recipes; plugin-supplied cards are rebuilt by setRecipe on every
 * visibility check. Cache them for GT6 machine cards and GT6 workbench cards.
 */
@Pseudo
@Mixin(targets = "mezz.jei.library.recipes.collect.RecipeTypeData", remap = false)
public abstract class JeiDynamicIngredientsMixin<T> implements DynamicRecipeIngredients.Cache {
    @Unique private static final long gtqualityneo$UNCACHED = Long.MIN_VALUE;
    @Shadow @Final private IRecipeCategory<T> recipeCategory;
    @Unique private final RecipeIngredientCache<Object, List<FocusLink>> gtqualityneo$ingredients = new RecipeIngredientCache<>();

    @Unique private long gtqualityneo$version(Object recipe) {
        if (recipe instanceof Recipe gtRecipe && recipeCategory instanceof DynamicRecipeIngredients.Category owner)
            return ((RecipeMapRevision) owner.gtqualityneo$recipeMap()).gtqualityneo$recipeResetVersion()
                + RecipeVariants.ingredientVersion(gtRecipe);
        // One stable holder per GT6 workbench recipe per JEI session; its ore lists change only with resources.
        if (recipe instanceof RecipeHolder<?> holder && holder.value() instanceof MergedCraftingRecipes.View) return 0;
        return gtqualityneo$UNCACHED;
    }

    @Inject(method = "getFocusLinks", at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$reuseIngredients(T recipe, CallbackInfoReturnable<List<FocusLink>> cir) {
        long version = gtqualityneo$version(recipe);
        if (version == gtqualityneo$UNCACHED) return;
        var cached = gtqualityneo$ingredients.get(recipe, version, DynamicRecipeIngredients.resourceVersion());
        if (cached != null) cir.setReturnValue(cached);
    }

    @Override public void gtqualityneo$cacheIngredients(Object recipe, List<?> links) {
        long version = gtqualityneo$version(recipe);
        if (version == gtqualityneo$UNCACHED) return;
        @SuppressWarnings("unchecked") var typed = (List<FocusLink>) links;
        gtqualityneo$ingredients.put(recipe, List.copyOf(typed), version, DynamicRecipeIngredients.resourceVersion());
    }
}
