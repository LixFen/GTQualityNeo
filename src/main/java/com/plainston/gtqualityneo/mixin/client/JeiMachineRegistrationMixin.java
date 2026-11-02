// 兼容说明：基于 GT6w alpha.17 的 GT6_JEI_Plugin；未来上游修改注册流程或入口签名后，本适配可能过时，需复核。
package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.machine.MachineRecipes;
import com.plainston.gtqualityneo.integration.GTQualityJeiPlugin;
import com.plainston.gtqualityneo.integration.jei.crafting.CookingRecipes;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IAdvancedRegistration;
import gregapi.recipes.Recipe.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Own machine pages and register GT6 cooking views normally; preserve its subtypes and crafting lookup. */
@Pseudo
@Mixin(targets = "gregapi.jei.GT6_JEI_Plugin", remap = false)
public abstract class JeiMachineRegistrationMixin {
    @Redirect(method = "registerCategories", at = @At(value = "FIELD",
        target = "Lgregapi/recipes/Recipe$RecipeMap;mNEIAllowed:Z"))
    private boolean gtqualityneo$ownMachineCategories(RecipeMap map) {
        return !GTQualityJeiPlugin.optimizeRecipeQueries() && map.mNEIAllowed;
    }

    @Redirect(method = "registerAdvanced", at = @At(value = "INVOKE", target =
        "Lmezz/jei/api/registration/IAdvancedRegistration;addSimpleRecipeManagerPlugin(Lmezz/jei/api/recipe/types/IRecipeType;Lmezz/jei/api/recipe/advanced/ISimpleRecipeManagerPlugin;)V"))
    private <T> void gtqualityneo$registerCookingOnce(IAdvancedRegistration registration, IRecipeType<T> type,
        ISimpleRecipeManagerPlugin<T> plugin) {
        if (!GTQualityJeiPlugin.optimizeRecipeQueries() || !CookingRecipes.defer(type, plugin))
            registration.addSimpleRecipeManagerPlugin(type, plugin);
    }

    @Inject(method = "showRecipeCategory", at = @At("HEAD"), cancellable = true)
    private static void gtqualityneo$openMachineCategory(String name, CallbackInfoReturnable<Boolean> cir) {
        if (GTQualityJeiPlugin.optimizeRecipeQueries()) cir.setReturnValue(MachineRecipes.show(name));
    }
}
