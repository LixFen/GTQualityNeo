package com.plainston.gtqualityneo.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.plainston.gtqualityneo.qol.CombustionAir;

import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorFluidBed;
import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorLiquid;
import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorSolid;

@Mixin(
    value = { MultiTileEntityGeneratorSolid.class, MultiTileEntityGeneratorLiquid.class,
        MultiTileEntityGeneratorFluidBed.class },
    remap = false)
public abstract class BurningBoxAirMixin {

    @Redirect(
        method = { "onTick2", "getStateRunningPossible" },
        at = @At(
            value = "INVOKE",
            target = "Lgregapi/util/WD;hasCollide(Lnet/minecraft/world/level/LevelAccessor;III)Z",
            remap = false),
        remap = false)
    private boolean allowClosedTrapDoorCollision(LevelAccessor world, int x, int y, int z) {
        return CombustionAir.hasCollision(world, x, y, z);
    }

    @Redirect(
        method = { "onTick2", "getStateRunningPossible" },
        at = @At(value = "INVOKE", target = "Lgregapi/util/WD;oxygen(Lnet/minecraft/world/level/Level;III)Z", remap = false),
        remap = false)
    private boolean allowClosedTrapDoorOxygen(Level world, int x, int y, int z) {
        return CombustionAir.hasOxygen(world, x, y, z);
    }
}
