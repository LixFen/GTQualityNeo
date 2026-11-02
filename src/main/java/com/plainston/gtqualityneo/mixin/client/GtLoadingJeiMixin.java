package com.plainston.gtqualityneo.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.plainston.gtqualityneo.client.loading.GtLoadingProgress;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "gregapi.jei.GT6_JEI_Plugin", remap = false)
public abstract class GtLoadingJeiMixin {
    @WrapMethod(method = "registerRecipes")
    private void gtqualityneo$recipes(IRecipeRegistration registration, Operation<Void> original) {
        try (var progress = GtLoadingProgress.begin("jei", 0)) { original.call(registration); }
    }
    @WrapMethod(method = "onRuntimeAvailable")
    private void gtqualityneo$runtime(IJeiRuntime runtime, Operation<Void> original) {
        try (var progress = GtLoadingProgress.begin("jei", 0)) { original.call(runtime); }
    }
}
