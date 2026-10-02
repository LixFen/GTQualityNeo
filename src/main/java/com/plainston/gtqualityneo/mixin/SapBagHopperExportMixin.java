package com.plainston.gtqualityneo.mixin;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.plainston.gtqualityneo.GTQualityNeo;

import gregtech.tileentity.tools.MultiTileEntitySapBag;

@Mixin(value = MultiTileEntitySapBag.class, remap = false)
public abstract class SapBagHopperExportMixin {

    @Inject(method = "getAccessibleSlotsFromSide2", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtquality$accessibleSlot(byte side, CallbackInfoReturnable<int[]> cir) {
        if (GTQualityNeo.SAP_BAG_EXTRACTION.get()) cir.setReturnValue(new int[] { 0 });
    }

    @Inject(method = "canExtractItem2", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtquality$allowExtraction(int slot, ItemStack stack, byte side, CallbackInfoReturnable<Boolean> cir) {
        if (GTQualityNeo.SAP_BAG_EXTRACTION.get()) cir.setReturnValue(slot == 0);
    }
}
