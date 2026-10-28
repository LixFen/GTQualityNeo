package com.plainston.gtqualityneo.mixin;

import com.plainston.gtqualityneo.integration.multiblock.PreviewWorld;
import gregapi.util.WD;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WD.class, remap = false)
public abstract class PreviewWorldMixin {
    @Inject(method = "chunkNow", at = @At("HEAD"), cancellable = true)
    private static void gtqualityneo$previewChunk(Level level, int x, int z, CallbackInfoReturnable<LevelChunk> cir) {
        if (level instanceof PreviewWorld preview) cir.setReturnValue(preview.chunk(x, z));
    }
}
