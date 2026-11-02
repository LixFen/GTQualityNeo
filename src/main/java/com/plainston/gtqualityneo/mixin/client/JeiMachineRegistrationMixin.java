// 兼容说明：基于 GT6w alpha.17 的 GT6_JEI_Plugin；未来上游修改注册流程或入口签名后，本适配可能过时，需复核。
package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.machine.MachineRecipes;
import gregapi.recipes.Recipe.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Leave GT6's subtypes, crafting and cooking registrations intact; only machines are owned here. */
@Pseudo
@Mixin(targets = "gregapi.jei.GT6_JEI_Plugin", remap = false)
public abstract class JeiMachineRegistrationMixin {
    @Redirect(method = "registerCategories", at = @At(value = "FIELD",
        target = "Lgregapi/recipes/Recipe$RecipeMap;mNEIAllowed:Z"))
    private boolean gtqualityneo$ownMachineCategories(RecipeMap map) { return false; }

    @Inject(method = "showRecipeCategory", at = @At("HEAD"), cancellable = true)
    private static void gtqualityneo$openMachineCategory(String name, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(MachineRecipes.show(name));
    }
}
