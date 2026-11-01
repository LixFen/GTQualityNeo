package com.plainston.gtqualityneo.integration.jade;

import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.data.CS;
import gregapi.data.FL;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.fluid.FluidTankGT;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.block.multitileentity.IMultiTileEntity;
import com.plainston.gtqualityneo.mixin.jade.DeviceDataAccessors;
import gregapi.data.IL;
import gregapi.oredict.OreDictPrefix;
import gregapi.tileentity.connectors.MultiTileEntityAxle;
import gregapi.tileentity.connectors.MultiTileEntityPipeFluid;
import gregapi.tileentity.connectors.MultiTileEntityWireElectric;
import gregapi.tileentity.machines.ITileEntityRunningActively;
import gregapi.tileentity.machines.ITileEntityRunningPassively;
import gregapi.tileentity.machines.ITileEntitySwitchableOnOff;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.tileentity.tank.TileEntityBase08Barrel;
import gregapi.tileentity.tank.TileEntityBase08FluidContainer;
import gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank;
import gregtech.tileentity.energy.converters.MultiTileEntityEngineSteam;
import gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam;
import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorFluidBed;
import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorLiquid;
import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorSolid;
import gregtech.tileentity.energy.generators.MultiTileEntityMotorLiquid;
import gregtech.tileentity.energy.reactors.MultiTileEntityReactorCore;
import gregtech.tileentity.misc.MultiTileEntityFluidSpring;
import gregtech.tileentity.misc.MultiTileEntityRock;
import gregtech.tileentity.multiblocks.MultiTileEntityCokeOven;
import gregtech.tileentity.multiblocks.MultiTileEntityCrucible;
import gregtech.tileentity.multiblocks.MultiTileEntityTank;
import gregtech.tileentity.plants.MultiTileEntityBush;
import gregtech.tileentity.tools.MultiTileEntityAnvil;
import gregtech.tileentity.tools.MultiTileEntityMixingBowl;
import gregtech.tileentity.tools.MultiTileEntityMold;
import gregtech.tileentity.tools.MultiTileEntitySiftingTable;
import gregtech.tileentity.tools.MultiTileEntitySmeltery;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import gregapi.tileentity.base.TileEntityBase01Root;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Special-device snapshots. Client rendering is kept in a separate class for dedicated servers. */
public enum GT6DetailsProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;
    static final String DATA = "gtqualityneo.details";
    static final Identifier UID = Identifier.fromNamespaceAndPath("gtqualityneo", "device_details");
    private static final String PARAMS = "gtquality.parameters";
    private static final String STATE_SUPPORTED = "gtquality.state.supported";
    private static final String STATE_ON = "gtquality.state.on";
    private static final String STATE_PASSIVE = "gtquality.state.passive";
    private static final String STATE_ACTIVE = "gtquality.state.active";

    @Override public Identifier getUid() { return UID; }

    // Registration is scoped to GT6 roots. Resolve supported devices on the server instead of excluding controllers on the client.

    @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity tile = accessor.getBlockEntity();
        boolean throughPart = tile instanceof MultiTileEntityMultiBlockPart;
        if (tile instanceof MultiTileEntityMultiBlockPart part) {
            Object controller = part.getTarget(true);
            if (!(controller instanceof BlockEntity target)) return;
            tile = target;
        }
        String kind = kind(tile);
        if (kind.isEmpty() || !(tile instanceof TileEntityBase01Root)) return;
        CompoundTag snapshot = new CompoundTag();
        writeHudTags(tile, snapshot);
        snapshot.putString("kind", kind);
        snapshot.putBoolean("through_part", throughPart);
        writeHudParameters(snapshot);
        writeMachineState(tile, snapshot);
        if (tile instanceof MultiTileEntityBoilerTank boiler) snapshot.putInt("gt.boiler.pressure", boiler.getVisualData() & 31);
        if (tile instanceof MultiTileEntityAxle axle) snapshot.putLong("gt.transfer.ru", axle.mTransferredLast);
        if (tile instanceof MultiTileEntityWireElectric wire) snapshot.putLong("gt.transfer.eu", wire.mWattageLast);
        if (tile instanceof TileEntityBase08FluidContainer container) snapshot.putLong("gtquality.capacity", container.mTank.capacity());
        if (tile instanceof TileEntityBase08Barrel barrel) snapshot.putLong("gtquality.capacity", barrel.mTank.capacity());
        if (tile instanceof MultiTileEntityTank tank) snapshot.putLong("gtquality.capacity", tank.mTank.capacity());
        if (tile instanceof MultiTileEntitySmeltery smeltery) snapshot.putLong("gtquality.max_temperature", smeltery.getTemperatureMax((byte) 0));
        if (tile instanceof MultiTileEntityCrucible crucible) snapshot.putLong("gtquality.max_temperature", crucible.getTemperatureMax((byte) 0));
        if (tile instanceof MultiTileEntityMold mold) {
            snapshot.putLong("gtquality.max_temperature", mold.getMoldMaxTemperature());
            OreDictPrefix prefix = mold.getMoldRecipe(snapshot.getIntOr("gt.mold", 0));
            snapshot.putString("mold_prefix", prefix == null ? "" : prefix.mNameInternal);
        }
        // Preserve modern item components and original slot indexes, rather than decoding legacy saved stacks.
        if (tile instanceof Container inventory) {
            ListTag items = new ListTag();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack == null || stack.isEmpty() || IL.Display_Fluid.equal(stack, true, true)) continue;
                CompoundTag item = new CompoundTag();
                item.putInt("s", slot);
                item.put("stack", accessor.encodeAsNbt(ItemStack.OPTIONAL_STREAM_CODEC, stack));
                items.add(item);
            }
            snapshot.put("gt.invlist", items);
        }
        data.put(DATA, snapshot);
    }

    private static String kind(BlockEntity tile) {
        if (tile instanceof MultiTileEntityCokeOven) return "MultiTileEntityCokeOven";
        if (tile instanceof MultiTileEntitySmeltery) return "MultiTileEntitySmeltery";
        if (tile instanceof MultiTileEntityCrucible) return "MultiTileEntityCrucible";
        if (tile instanceof MultiTileEntityMold) return "MultiTileEntityMold";
        if (tile instanceof MultiTileEntityBoilerTank) return "MultiTileEntityBoilerTank";
        if (tile instanceof MultiTileEntityGeneratorSolid) return "MultiTileEntityGeneratorSolid";
        if (tile instanceof MultiTileEntityGeneratorFluidBed) return "MultiTileEntityGeneratorFluidBed";
        if (tile instanceof MultiTileEntityGeneratorLiquid) return "MultiTileEntityGeneratorLiquid";
        if (tile instanceof MultiTileEntityRock) return "MultiTileEntityRock";
        if (tile instanceof TileEntityBase08FluidContainer) return "TileEntityBase08FluidContainer";
        if (tile instanceof MultiTileEntityAnvil) return "MultiTileEntityAnvil";
        if (tile instanceof MultiTileEntityEngineSteam) return "MultiTileEntityEngineSteam";
        if (tile instanceof MultiTileEntityTurbineSteam) return "MultiTileEntityTurbineSteam";
        if (tile instanceof MultiTileEntityAxle) return "MultiTileEntityAxle";
        if (tile instanceof MultiTileEntityWireElectric) return "MultiTileEntityWireElectric";
        if (tile instanceof MultiTileEntityFluidSpring) return "MultiTileEntityFluidSpring";
        if (tile instanceof MultiTileEntityMixingBowl) return "MultiTileEntityMixingBowl";
        if (tile instanceof TileEntityBase08Barrel) return "TileEntityBase08Barrel";
        if (tile instanceof MultiTileEntityTank) return "MultiTileEntityTank";
        if (tile instanceof MultiTileEntityPipeFluid) return "MultiTileEntityPipeFluid";
        if (tile instanceof MultiTileEntitySiftingTable) return "MultiTileEntitySiftingTable";
        if (tile instanceof MultiTileEntityMotorLiquid) return "MultiTileEntityMotorLiquid";
        if (tile instanceof MultiTileEntityReactorCore) return "MultiTileEntityReactorCore";
        if (tile instanceof MultiTileEntityBush) return "MultiTileEntityBush";
        return "";
    }

    static void writeHudTags(BlockEntity tile, CompoundTag target) {
        if (tile instanceof IMultiTileEntity mte) target.putShort("gt.mte.id", mte.getMultiTileEntityID());
        if (tile instanceof MultiTileEntityMold) {
            var mold = (DeviceDataAccessors.Mold) tile;
            UT.NBT.setNumber(target, "gt.temperature", mold.gtqualityneo$temperature());
            UT.NBT.setNumber(target, "gt.mold", mold.gtqualityneo$shape());
            target.putByte("gt.connection", mold.gtqualityneo$directions());
            UT.NBT.setBoolean(target, "gt.mode", mold.gtqualityneo$redstone());
        } else if (tile instanceof MultiTileEntitySmeltery || tile instanceof MultiTileEntityCrucible) {
            var melting = (DeviceDataAccessors.Melting) tile;
            UT.NBT.setNumber(target, "gt.temperature", melting.gtqualityneo$temperature());
            OreDictMaterialStack.saveList("gt.materials", target, melting.gtqualityneo$materials());
        } else if (tile instanceof MultiTileEntityBoilerTank) {
            UT.NBT.setNumber(target, "gt.energy", ((DeviceDataAccessors.Energy) tile).gtqualityneo$energy());
            short efficiency = ((DeviceDataAccessors.Efficiency) tile).gtqualityneo$efficiency();
            if (efficiency != 10000) target.putShort("gt.eff", efficiency);
            writeTanks(target, "gt.tank.", ((DeviceDataAccessors.Boiler) tile).gtqualityneo$tanks());
        } else if (tile instanceof MultiTileEntityGeneratorSolid generator) {
            UT.NBT.setNumber(target, "gt.energy", ((DeviceDataAccessors.Energy) tile).gtqualityneo$energy());
            UT.NBT.setBoolean(target, "gt.active", generator.getStateRunningActively());
        } else if (tile instanceof MultiTileEntityGeneratorFluidBed generator) {
            UT.NBT.setNumber(target, "gt.energy", ((DeviceDataAccessors.Energy) tile).gtqualityneo$energy());
            UT.NBT.setBoolean(target, "gt.active", generator.getStateRunningActively());
            ((DeviceDataAccessors.FuelTank) tile).gtqualityneo$fuelTank().writeToNBT(target, "gt.tank");
        } else if (tile instanceof MultiTileEntityGeneratorLiquid generator) {
            UT.NBT.setBoolean(target, "gt.active", generator.getStateRunningActively());
            ((DeviceDataAccessors.FuelTank) tile).gtqualityneo$fuelTank().writeToNBT(target, "gt.tank");
        } else if (tile instanceof MultiTileEntityRock rock) {
            ST.save(target, "gt.value", rock.mRock);
        } else if (tile instanceof TileEntityBase08FluidContainer container) {
            container.mTank.writeToNBT(target, "gt.tank");
        } else if (tile instanceof TileEntityBase08Barrel barrel) {
            barrel.mTank.writeToNBT(target, "gt.tank");
        } else if (tile instanceof MultiTileEntityTank tank) {
            tank.mTank.writeToNBT(target, "gt.tank");
        } else if (tile instanceof MultiTileEntityAnvil anvil) {
            UT.NBT.setNumber(target, "gt.durability", anvil.mDurability);
            target.putByte("gt.facing", anvil.mFacing);
        } else if (tile instanceof MultiTileEntityEngineSteam engine) {
            UT.NBT.setNumber(target, CS.NBT_ENERGY, ((DeviceDataAccessors.Energy) tile).gtqualityneo$energy());
            target.putByte(CS.NBT_VISUAL, ((DeviceDataAccessors.SteamEngine) tile).gtqualityneo$state());
            target.putShort(CS.NBT_EFFICIENCY, ((DeviceDataAccessors.Efficiency) tile).gtqualityneo$efficiency());
            UT.NBT.setBoolean(target, CS.NBT_ACTIVE, ((DeviceDataAccessors.SteamEngine) tile).gtqualityneo$active());
            UT.NBT.setBoolean(target, CS.NBT_STOPPED, !engine.getStateOnOff());
        } else if (tile instanceof MultiTileEntityTurbineSteam turbine) {
            UT.NBT.setNumber(target, "gt.output.su", turbine.mEnergyProducedNextTick);
            turbine.mTank.writeToNBT(target, "gt.tank.0");
        } else if (tile instanceof MultiTileEntityFluidSpring spring) {
            FL.save(target, "gt.spring", spring.mFluid);
        } else if (tile instanceof MultiTileEntityMixingBowl) {
            var mixing = (DeviceDataAccessors.Mixing) tile;
            writeTanks(target, "gt.tank.in.", mixing.gtqualityneo$inputs());
            writeTanks(target, "gt.tank.out.", mixing.gtqualityneo$outputs());
        } else if (tile instanceof MultiTileEntityPipeFluid pipe) {
            writeTanks(target, "gt.tank.", pipe.mTanks);
            if (pipe.mTanks.length > 8) target.putByte("gt.mlast.8", pipe.mLastReceivedFrom[8]);
        } else if (tile instanceof MultiTileEntityMotorLiquid motor) {
            UT.NBT.setNumber(target, "gt.energy", motor.mEnergy);
            writeTanks(target, "gt.tank.", motor.mTanks);
        } else if (tile instanceof MultiTileEntityReactorCore reactor) {
            writeTanks(target, "gt.tank.", reactor.mTanks);
            UT.NBT.setBoolean(target, "gt.stopped", reactor.mStopped);
            for (int i = 0; i < 4; i++) UT.NBT.setNumber(target, "gt.value.o." + i, reactor.oNeutronCounts[i]);
        } else if (tile instanceof MultiTileEntityBush bush) {
            target.putByte("gt.state", bush.mStage);
            target.putByte("gt.progress", bush.mGrowth);
            ST.save(target, "gt.value", bush.mBerry);
        }
    }

    private static void writeTanks(CompoundTag target, String prefix, FluidTankGT[] tanks) {
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(target, prefix + i);
    }

    private static void copyTags(CompoundTag source, CompoundTag target, String... keys) {
        for (String key : keys) {
            if (source.contains(key)) target.put(
                key,
                source.get(key)
                    .copy());
        }
    }

    private static void writeHudParameters(CompoundTag data) {
        int mteId = data.getIntOr("gt.mte.id", 0);
        if (mteId <= 0) return;
        try {
            MultiTileEntityRegistry registry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
            gregapi.block.multitileentity.MultiTileEntityClassContainer container = registry == null ? null
                : registry.getClassContainer(mteId);
            if (container == null || container.mParameters == null) return;
            CompoundTag parameters = new CompoundTag();
            copyTags(
                container.mParameters,
                parameters,
                "gt.capacity.su",
                "gt.capacity",
                "gt.output.su",
                "gt.output",
                "gt.input");
            data.put(PARAMS, parameters);
        } catch (RuntimeException ignored) {
            // Keep the live HUD fields useful if a registry entry is unavailable.
        }
    }

    private static void writeMachineState(BlockEntity tile, CompoundTag data) {
        if (!(tile instanceof ITileEntityRunningActively) && !(tile instanceof ITileEntityRunningPassively)
            && !(tile instanceof ITileEntitySwitchableOnOff)) return;

        data.putBoolean(STATE_SUPPORTED, true);
        if (tile instanceof ITileEntitySwitchableOnOff) {
            data.putBoolean(STATE_ON, ((ITileEntitySwitchableOnOff) tile).getStateOnOff());
        } else {
            data.putBoolean(STATE_ON, !data.contains(CS.NBT_STOPPED));
        }
        data.putBoolean(
            STATE_PASSIVE,
            tile instanceof ITileEntityRunningPassively
                ? ((ITileEntityRunningPassively) tile).getStateRunningPassively()
                : data.getBooleanOr(STATE_ON, false));
        if (tile instanceof ITileEntityRunningActively) {
            data.putBoolean(STATE_ACTIVE, ((ITileEntityRunningActively) tile).getStateRunningActively());
        }
    }
}
