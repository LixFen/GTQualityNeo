package com.plainston.gtqualityneo.integration.jade;

import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.data.CS;
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
        if (kind.isEmpty() || !(tile instanceof TileEntityBase01Root root)) return;
        CompoundTag saved = new CompoundTag();
        root.writeToNBT(saved);
        CompoundTag snapshot = new CompoundTag();
        copyHudTags(tile, saved, snapshot);
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
            OreDictPrefix prefix = mold.getMoldRecipe(saved.getIntOr("gt.mold", 0));
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

    private static void copyHudTags(BlockEntity tile, CompoundTag source, CompoundTag target) {
        copyTags(source, target, "gt.mte.id");
        if (tile instanceof MultiTileEntityMold) {
            copyTags(source, target, "gt.temperature", "gt.mold", "gt.connection", "gt.mode");
        } else if (tile instanceof MultiTileEntitySmeltery || tile instanceof MultiTileEntityCrucible) {
            copyTags(source, target, "gt.temperature", "gt.materials");
        } else if (tile instanceof MultiTileEntityBoilerTank) {
            copyTags(source, target, "gt.energy", "gt.tank.0", "gt.tank.1", "gt.eff");
        } else if (tile instanceof MultiTileEntityGeneratorSolid) {
            copyTags(source, target, "gt.energy", "gt.active", "gt.invlist");
        } else if (tile instanceof MultiTileEntityGeneratorFluidBed) {
            copyTags(source, target, "gt.energy", "gt.active", "gt.invlist", "gt.tank");
        } else if (tile instanceof MultiTileEntityGeneratorLiquid) {
            copyTags(source, target, "gt.active", "gt.tank");
        } else if (tile instanceof MultiTileEntityRock) {
            copyTags(source, target, "gt.value");
        } else if (tile instanceof TileEntityBase08FluidContainer || tile instanceof TileEntityBase08Barrel
            || tile instanceof MultiTileEntityTank) {
                copyTags(source, target, "gt.tank");
            } else if (tile instanceof MultiTileEntityMultiBlockPart) {
                copyTags(source, target, "gt.target", "gt.target.x", "gt.target.y", "gt.target.z");
            } else if (tile instanceof MultiTileEntityAnvil) {
                copyTags(source, target, "gt.durability", "gt.facing");
            } else if (tile instanceof MultiTileEntityEngineSteam) {
                copyTags(
                    source,
                    target,
                    CS.NBT_ENERGY,
                    CS.NBT_VISUAL,
                    CS.NBT_EFFICIENCY,
                    CS.NBT_ACTIVE,
                    CS.NBT_STOPPED);
            } else if (tile instanceof MultiTileEntityTurbineSteam) {
                copyTags(source, target, "gt.output.su", "gt.tank.0");
            } else if (tile instanceof MultiTileEntityFluidSpring) {
                copyTags(source, target, "gt.spring");
            } else if (tile instanceof MultiTileEntityMixingBowl) {
                copyTags(source, target, "gt.invlist");
                for (int i = 0; i < 6; i++) copyTags(source, target, "gt.tank.in." + i);
                for (int i = 0; i < 2; i++) copyTags(source, target, "gt.tank.out." + i);
            } else if (tile instanceof MultiTileEntityPipeFluid) {
                copyTags(source, target, "gt.mlast.8");
                for (int i = 0; i < 9; i++) copyTags(source, target, "gt.tank." + i);
            } else if (tile instanceof MultiTileEntitySiftingTable) {
                copyTags(source, target, "gt.invlist");
            } else if (tile instanceof MultiTileEntityMotorLiquid) {
                copyTags(source, target, "gt.energy", "gt.tank.0", "gt.tank.1");
            } else if (tile instanceof MultiTileEntityReactorCore) {
                copyTags(source, target, "gt.tank.0", "gt.tank.1", "gt.stopped", "gt.invlist");
                for (int i = 0; i < 4; i++) copyTags(source, target, "gt.value.o." + i);
            } else if (tile instanceof MultiTileEntityBush) {
                copyTags(source, target, "gt.state", "gt.progress", "gt.value");
            }
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
