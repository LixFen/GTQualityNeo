package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.RecipeConfigBatch;
import gregapi.config.ModConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModConfigSpec.class, remap = false)
public abstract class JeiRecipeConfigMixin {
    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$batchQueryWrites(CallbackInfo ci) {
        if (RecipeConfigBatch.defer((ModConfigSpec) (Object) this)) ci.cancel();
    }
}
