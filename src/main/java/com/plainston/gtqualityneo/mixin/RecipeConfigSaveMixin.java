// Compatibility: GT6w alpha.17 handlers save the entire config after each material/prefix batch.
package com.plainston.gtqualityneo.mixin;

import com.plainston.gtqualityneo.integration.jei.machine.RecipeConfigBatch;
import gregapi.config.ModConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModConfigSpec.class, remap = false)
public abstract class RecipeConfigSaveMixin {
    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$batchRecipeSaves(CallbackInfo ci) {
        if (RecipeConfigBatch.defer((ModConfigSpec) (Object) this)) ci.cancel();
    }
}
