package com.plainston.gtqualityneo.mixin.jade;

import gregapi.fluid.FluidTankGT;
import gregapi.oredict.OreDictMaterialStack;
import gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank;
import gregtech.tileentity.energy.converters.MultiTileEntityEngineSteam;
import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorFluidBed;
import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorLiquid;
import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorSolid;
import gregtech.tileentity.multiblocks.MultiTileEntityCrucible;
import gregtech.tileentity.tools.MultiTileEntityMixingBowl;
import gregtech.tileentity.tools.MultiTileEntityMold;
import gregtech.tileentity.tools.MultiTileEntitySmeltery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

/** Read only the live fields used by Jade, without invoking device persistence or inventory codecs. */
public final class DeviceDataAccessors {
    private DeviceDataAccessors() {}

    @Mixin(value = {MultiTileEntityBoilerTank.class, MultiTileEntityEngineSteam.class,
        MultiTileEntityGeneratorSolid.class, MultiTileEntityGeneratorFluidBed.class}, remap = false)
    public interface Energy {
        @Accessor("mEnergy") long gtqualityneo$energy();
    }

    @Mixin(value = {MultiTileEntityBoilerTank.class, MultiTileEntityEngineSteam.class}, remap = false)
    public interface Efficiency {
        @Accessor("mEfficiency") short gtqualityneo$efficiency();
    }

    @Mixin(value = MultiTileEntityBoilerTank.class, remap = false)
    public interface Boiler {
        @Accessor("mTanks") FluidTankGT[] gtqualityneo$tanks();
    }

    @Mixin(value = {MultiTileEntityGeneratorLiquid.class, MultiTileEntityGeneratorFluidBed.class}, remap = false)
    public interface FuelTank {
        @Accessor("mTank") FluidTankGT gtqualityneo$fuelTank();
    }

    @Mixin(value = MultiTileEntityMold.class, remap = false)
    public interface Mold {
        @Accessor("mTemperature") long gtqualityneo$temperature();
        @Accessor("mShape") int gtqualityneo$shape();
        @Accessor("mAutoPullDirections") byte gtqualityneo$directions();
        @Accessor("mUseRedstone") boolean gtqualityneo$redstone();
    }

    @Mixin(value = {MultiTileEntitySmeltery.class, MultiTileEntityCrucible.class}, remap = false)
    public interface Melting {
        @Accessor("mTemperature") long gtqualityneo$temperature();
        @Accessor("mContent") List<OreDictMaterialStack> gtqualityneo$materials();
    }

    @Mixin(value = MultiTileEntityEngineSteam.class, remap = false)
    public interface SteamEngine {
        @Accessor("mState") byte gtqualityneo$state();
        @Accessor("mActive") boolean gtqualityneo$active();
    }

    @Mixin(value = MultiTileEntityMixingBowl.class, remap = false)
    public interface Mixing {
        @Accessor("mTanksInput") FluidTankGT[] gtqualityneo$inputs();
        @Accessor("mTanksOutput") FluidTankGT[] gtqualityneo$outputs();
    }
}
