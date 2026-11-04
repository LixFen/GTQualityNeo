// GT6w alpha.17 routes its recipe buttons through JEI-specific entry points.
package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.rei.GTQualityReiPlugin;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class ReiRecipeGuiMixin {
    private ReiRecipeGuiMixin() {}

    @Pseudo
    @Mixin(targets = "gregapi.gui.ContainerClient", remap = false)
    public abstract static class Container {
        @Shadow public String mNEI;

        @Inject(method = "openRecipesForThisGUI", at = @At("HEAD"), cancellable = true)
        private void gtqualityneo$openMachineRecipes(CallbackInfoReturnable<Boolean> cir) {
            if (ModList.get().isLoaded("roughlyenoughitems") && !ModList.get().isLoaded("jei"))
                cir.setReturnValue(GTQualityReiPlugin.show(mNEI));
        }
    }

    @Pseudo
    @Mixin(targets = "gregapi.GT_API_Proxy_Client", remap = false)
    public abstract static class Proxy {
        @Inject(method = "openRecipeGui", at = @At("HEAD"), cancellable = true)
        private void gtqualityneo$openRecipeMap(String name, CallbackInfoReturnable<Boolean> cir) {
            if (ModList.get().isLoaded("roughlyenoughitems") && !ModList.get().isLoaded("jei"))
                cir.setReturnValue(GTQualityReiPlugin.show(name));
        }
    }
}
