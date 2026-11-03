// Compatibility: GT6w alpha.17 eagerly registers these loaders through native recipe collision checks.
package com.plainston.gtqualityneo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.plainston.gtqualityneo.integration.jei.machine.RecipeRegistrationFilter;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = {"gregtech.loaders.c.Loader_Recipes_Ores", "gregtech.loaders.c.Loader_Recipes_Other"}, remap = false)
public abstract class RecipeLoaderRegistrationMixin {
    @WrapMethod(method = "run")
    private void gtqualityneo$filterLoaderRegistrations(Operation<Void> original) {
        try (var batch = RecipeRegistrationFilter.beginAll()) { original.call(); }
    }
}
