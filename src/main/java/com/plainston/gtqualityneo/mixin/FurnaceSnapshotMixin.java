// Compatibility: GT6w alpha.17 snapshot's first Stream.toList is its copied blacklist.
package com.plainston.gtqualityneo.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.plainston.gtqualityneo.integration.loading.FurnaceSnapshotKeys;
import gregapi.recipes.FurnaceRecipes;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = FurnaceRecipes.class, remap = false)
public abstract class FurnaceSnapshotMixin {
    @ModifyExpressionValue(method = "snapshot", at = @At(value = "INVOKE", ordinal = 0,
        target = "Ljava/util/stream/Stream;toList()Ljava/util/List;"))
    private List<ItemStack> gtqualityneo$indexBlacklist(List<ItemStack> keys) {
        return new FurnaceSnapshotKeys(keys);
    }

    @Inject(method = "matchesKeys", at = @At("HEAD"), cancellable = true)
    private static void gtqualityneo$snapshotKeys(ItemStack input, List<ItemStack> keys,
        CallbackInfoReturnable<Boolean> cir) {
        if (keys instanceof FurnaceSnapshotKeys indexed) cir.setReturnValue(indexed.matches(input));
    }
}
