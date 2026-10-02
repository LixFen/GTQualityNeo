package com.plainston.gtqualityneo.mixin;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.util.WD;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WD.class, remap = false)
public abstract class ObstructionMixin {
    @Inject(method = "obstructed", at = @At("HEAD"), cancellable = true)
    private static void gtquality$interaction(LevelAccessor level, int x, int y, int z, byte side, CallbackInfoReturnable<Boolean> cir) {
        if (GTQualityNeo.OBSTRUCTED_INTERACTION.get()) cir.setReturnValue(false);
    }
}
