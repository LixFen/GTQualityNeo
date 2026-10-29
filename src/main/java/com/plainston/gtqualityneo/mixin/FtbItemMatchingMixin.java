package com.plainston.gtqualityneo.mixin;

import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem.ComponentMatchType;
import gregapi.data.CS.ModIDs;
import gregapi.util.ST;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** GT6 material variants share an Item; their subtype remains significant in FTBQ's default mode. */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem", remap = false)
public abstract class FtbItemMatchingMixin {
    @Inject(method = "areItemStacksEqual", at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$matchSubtype(ItemStack required, ItemStack offered, ComponentMatchType matchType,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (matchType != ComponentMatchType.NONE || required.isEmpty()) return;
        if (!ModIDs.isGregNamespace(BuiltInRegistries.ITEM.getKey(required.getItem()).getNamespace())) return;
        cir.setReturnValue(!offered.isEmpty() && required.getItem() == offered.getItem()
            && ST.meta_(required) == ST.meta_(offered));
    }
}
