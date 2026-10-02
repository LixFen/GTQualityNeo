package com.plainston.gtqualityneo.mixin;

import gregapi.data.CS;
import gregtech.tileentity.tools.MultiTileEntityMold;
import gregapi.oredict.OreDictMaterialStack;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MultiTileEntityMold.class, remap = false)
public abstract class MoldStateMixin {
    @Shadow protected OreDictMaterialStack mContent;
    @Inject(method = "readFromNBT2", at = @At("TAIL"))
    private void gtquality$emptyMold(CompoundTag tag, CallbackInfo ci) {
        if (tag.getCompoundOrEmpty(CS.NBT_MATERIALS).isEmpty()) mContent = null;
    }
}
