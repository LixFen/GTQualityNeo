package com.plainston.gtqualityneo.mixin;

import com.plainston.gtqualityneo.qol.CollisionShapes;
import gregapi.block.multitileentity.MultiTileEntityBlock;
import gregapi.data.CS;
import gregtech.tileentity.inventories.MultiTileEntityHopper;
import gregtech.tileentity.inventories.MultiTileEntityQueueHopper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {MultiTileEntityBlock.class, gregapi.block.multitileentity.MultiTileEntityBlockInternal.class}, remap = false)
public abstract class HopperShapeMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void gtquality$shape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context,
                                CallbackInfoReturnable<VoxelShape> cir) {
        var tile = world.getBlockEntity(pos);
        if (!(tile instanceof MultiTileEntityHopper || tile instanceof MultiTileEntityQueueHopper)) return;
        cir.setReturnValue(CollisionShapes.hopper((gregapi.tileentity.base.TileEntityBase09FacingSingle) tile));
    }

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void gtquality$toolSelection(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context,
                                        CallbackInfoReturnable<VoxelShape> cir) {
        var tile = world.getBlockEntity(pos);
        if (!(tile instanceof MultiTileEntityHopper || tile instanceof MultiTileEntityQueueHopper)) return;
        if (context instanceof EntityCollisionContext entity && entity.getEntity() instanceof Player player) {
            var held = player.getMainHandItem();
            if (CS.ToolsGT.contains(CS.TOOL_wrench, held) || CS.ToolsGT.contains(CS.TOOL_monkeywrench, held)
                || CS.ToolsGT.contains(CS.TOOL_screwdriver, held)) {
                cir.setReturnValue(Shapes.block());
                return;
            }
        }
        cir.setReturnValue(CollisionShapes.hopper((gregapi.tileentity.base.TileEntityBase09FacingSingle) tile));
    }
}
