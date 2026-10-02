package com.plainston.gtqualityneo.qol;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import gregapi.util.WD;

public final class CombustionAir {

    private CombustionAir() {}

    public static boolean hasCollision(LevelAccessor world, int x, int y, int z) {
        return !isClosedTrapDoor(world, x, y, z) && WD.hasCollide(world, x, y, z);
    }

    public static boolean hasOxygen(Level world, int x, int y, int z) {
        return isClosedTrapDoor(world, x, y, z) || WD.oxygen(world, x, y, z);
    }

    private static boolean isClosedTrapDoor(LevelAccessor world, int x, int y, int z) {
        var state = world.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
        Block block = state.getBlock();
        return block instanceof TrapDoorBlock && !state.getValue(TrapDoorBlock.OPEN) && !state.getValue(TrapDoorBlock.WATERLOGGED);
    }
}
