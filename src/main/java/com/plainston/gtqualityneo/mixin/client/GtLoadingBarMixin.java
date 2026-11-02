package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.client.loading.GtLoadingProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bridge GT6's legacy Forge progress calls without depending on its obsolete ProgressManager. */
@Mixin(targets = "gregapi.util.UT$LoadingBar", remap = false)
public abstract class GtLoadingBarMixin {
    @Inject(method = "start", at = @At("HEAD"))
    private static void gtqualityneo$start(String title, int total, CallbackInfoReturnable<Boolean> cir) {
        GtLoadingProgress.beginBar(title, total);
    }
    @Inject(method = "step", at = @At("HEAD"))
    private static void gtqualityneo$step(Object detail, CallbackInfoReturnable<Boolean> cir) {
        GtLoadingProgress.stepBar(detail == null ? "" : detail.toString());
    }
    @Inject(method = "finish", at = @At("HEAD"))
    private static void gtqualityneo$finish(CallbackInfoReturnable<Boolean> cir) {
        GtLoadingProgress.finishBar();
    }
}
