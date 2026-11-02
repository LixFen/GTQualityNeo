// 兼容说明：依赖 GT6w alpha.17 / 当前 JEI 的接口；未来上游更新后本适配可能过时，需复核。
package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.cache.RecipeConfigBatch;
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
