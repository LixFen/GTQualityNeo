package com.plainston.gtqualityneo.mixin;

import com.plainston.gtqualityneo.qol.CollisionShapes;
import gregapi.tileentity.connectors.TileEntityBase10ConnectorRendered;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityBase10ConnectorRendered.class, remap = false)
public abstract class SmallCoverMixin {
    @Inject(method = "shrunkBox", at = @At("HEAD"), cancellable = true)
    private void gtquality$bounds(CallbackInfoReturnable<float[]> cir) {
        var bounds = CollisionShapes.smallCoverBounds((TileEntityBase10ConnectorRendered) (Object) this);
        if (bounds != null) cir.setReturnValue(bounds);
    }
}
