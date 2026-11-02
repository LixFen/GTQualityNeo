// 兼容说明：依赖 GT6w alpha.17 / 当前 JEI 的接口；未来上游更新后本适配可能过时，需复核。
package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.compat.DynamicRecipeIngredients;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mezz.jei.library.ingredients.RecipeIngredientSupplier.FocusLink;
import mezz.jei.library.recipes.collect.RecipeTypeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import java.util.List;

@Pseudo
@Mixin(targets = "mezz.jei.library.recipes.RecipeManagerInternal", remap = false)
public abstract class JeiDynamicIngredientsExtractMixin {
    // JEI's optimized bytecode reuses parameter slots, including `this`, before returning.
    // Keep the original arguments in a wrapper rather than loading those slots at RETURN.
    @WrapMethod(method = "getFocusLinks")
    private <T> List<FocusLink> gtqualityneo$rememberIngredients(RecipeTypeData<T> data, T recipe,
        Operation<List<FocusLink>> original) {
        var links = original.call(data, recipe);
        ((DynamicRecipeIngredients.Cache) data).gtqualityneo$cacheIngredients(recipe, links);
        return links;
    }
}
