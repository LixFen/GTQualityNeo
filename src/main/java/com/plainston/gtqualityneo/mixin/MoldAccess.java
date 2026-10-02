package com.plainston.gtqualityneo.mixin;

import gregapi.oredict.OreDictMaterialStack;
import gregtech.tileentity.tools.MultiTileEntityMold;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MultiTileEntityMold.class, remap = false)
public interface MoldAccess {
    @Accessor("mShape") int gtquality$getShape();
    @Accessor("mShape") void gtquality$setShape(int shape);
    @Accessor("mContent") OreDictMaterialStack gtquality$getContent();
}
