package com.plainston.gtqualityneo.mixin;

import com.plainston.gtqualityneo.integration.multiblock.PreviewCapture;
import gregapi.tileentity.multiblocks.ITileEntityMultiBlockController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ITileEntityMultiBlockController.Util.class, remap = false)
public abstract class MultiblockPreviewCaptureMixin {
    @Inject(method = "checkAndSetTarget(Lgregapi/tileentity/multiblocks/ITileEntityMultiBlockController;IIIIIIILnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/Container;)Z",
        at = @At("HEAD"), cancellable = true)
    private static void gtqualityneo$capture(ITileEntityMultiBlockController controller, int x, int y, int z,
        int id, int registry, int design, int mode, BlockPos clickedAt, Entity player, Container inventory,
        CallbackInfoReturnable<Boolean> cir) {
        Boolean result = PreviewCapture.visit(controller, x, y, z, id, registry, design, mode);
        if (result != null) cir.setReturnValue(result);
    }
}
