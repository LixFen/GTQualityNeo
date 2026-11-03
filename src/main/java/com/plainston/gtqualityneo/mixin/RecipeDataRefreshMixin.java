// Compatibility: GT6w alpha.17 repeats finalizeRecipeLoading with no intervening recipe changes.
package com.plainston.gtqualityneo.mixin;

import gregapi.GT_API;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GT_API.class, remap = false)
public abstract class RecipeDataRefreshMixin {
    @Redirect(method = "onLevelLoadEarlyItemInit", at = @At(value = "INVOKE", ordinal = 1, target =
        "Lnet/minecraft/world/item/crafting/RecipeManager;finalizeRecipeLoading(Lnet/minecraft/world/flag/FeatureFlagSet;)V"))
    private void gtqualityneo$skipDuplicateRefresh(RecipeManager manager, FeatureFlagSet flags) {
        // The preceding call already rebuilt inputs and displays after datapack suppression.
    }
}
