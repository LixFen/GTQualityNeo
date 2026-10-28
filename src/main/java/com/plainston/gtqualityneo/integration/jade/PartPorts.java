package com.plainston.gtqualityneo.integration.jade;

import gregapi.code.TagData;
import gregapi.data.CS;
import gregapi.tileentity.machines.MultiTileEntityBasicMachine;
import gregapi.tileentity.base.TileEntityBase09FacingSingle;
import gregapi.tileentity.multiblocks.IMultiBlockEnergy;
import gregapi.tileentity.multiblocks.IMultiBlockFluidHandler;
import gregapi.tileentity.multiblocks.IMultiBlockInventory;
import gregapi.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.tileentity.multiblocks.TileEntityBase11MultiBlockConverter;
import gregtech.tileentity.multiblocks.MultiTileEntityBedrockDrill;
import gregtech.tileentity.multiblocks.MultiTileEntityCrucible;
import gregtech.tileentity.multiblocks.MultiTileEntityCryoDistillationTower;
import gregtech.tileentity.multiblocks.MultiTileEntityDistillationTower;
import gregtech.tileentity.multiblocks.MultiTileEntityFusionReactor;
import gregtech.tileentity.multiblocks.MultiTileEntityLargeBoiler;
import gregtech.tileentity.multiblocks.MultiTileEntityLargeHeatExchanger;
import gregtech.tileentity.multiblocks.MultiTileEntityLargeTurbineGas;
import gregtech.tileentity.multiblocks.MultiTileEntityLargeTurbineSteam;
import gregtech.tileentity.multiblocks.MultiTileEntityTank;
import gregtech.tileentity.multiblocks.MultiTileEntityFermenter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PartPorts {
    private static final String PART_ITEM_INPUT_SIDES = "gtquality.part.item_input_sides";
    private static final String PART_ITEM_OUTPUT_SIDES = "gtquality.part.item_output_sides";
    private static final String PART_FLUID_INPUT_SIDES = "gtquality.part.fluid_input_sides";
    private static final String PART_FLUID_OUTPUT_SIDES = "gtquality.part.fluid_output_sides";
    private static final String PART_ENERGY_INPUT_TYPES = "gtquality.part.energy_input_types";
    private static final String PART_ENERGY_OUTPUT_TYPES = "gtquality.part.energy_output_types";
    private static final String PART_ITEM_AUTO_OUTPUT_SIDES = "gtquality.part.item_auto_output_sides";
    private static final String PART_ITEM_AUTO_OUTPUT_DISABLED_SIDES = "gtquality.part.item_auto_output_disabled_sides";
    private static final String PART_FLUID_AUTO_OUTPUT_SIDES = "gtquality.part.fluid_auto_output_sides";
    private static final String PART_FLUID_AUTO_OUTPUT_DISABLED_SIDES = "gtquality.part.fluid_auto_output_disabled_sides";
    private static final String PART_ENERGY_AUTO_OUTPUT_SIDES = "gtquality.part.energy_auto_output_sides";

    public static void write(MultiTileEntityMultiBlockPart part, CompoundTag data) {
        int mode = part.mMode;
        ITileEntityMultiBlockController target = part.getTarget(CS.T);
        if (target == null) return;
        BlockEntity targetTile = (BlockEntity) target;
        MultiTileEntityBasicMachine machine = targetTile instanceof MultiTileEntityBasicMachine
            ? (MultiTileEntityBasicMachine) targetTile : null;
        byte facing = CS.SIDE_ANY;
        if (targetTile instanceof TileEntityBase09FacingSingle) {
            // Multiblock controllers use bit 3 of direction data for the formed state.
            facing = (byte) (((TileEntityBase09FacingSingle) targetTile).getDirectionData() & 7);
        }

        int itemInputSides = 0;
        int itemOutputSides = 0;
        int fluidInputSides = 0;
        int fluidOutputSides = 0;
        int itemAutoOutputSides = 0;
        int itemAutoOutputDisabledSides = 0;
        int fluidAutoOutputSides = 0;
        int fluidAutoOutputDisabledSides = 0;

        if (target instanceof IMultiBlockInventory) {
            IMultiBlockInventory inventory = (IMultiBlockInventory) target;
            for (byte side = 0; side < 6; side++) {
                int[] slots = inventory.getAccessibleSlotsFromSide(part, side);
                if (slots == null || slots.length == 0) continue;
                boolean input = false;
                boolean output = false;
                for (int slot : slots) {
                    if (machine != null) {
                        int firstOutputSlot = machine.mRecipes.mInputItemsCount;
                        int afterOutputSlots = firstOutputSlot + machine.mRecipes.mOutputItemsCount;
                        if (slot >= 0 && slot < firstOutputSlot) input = true;
                        else if (slot >= firstOutputSlot && slot < afterOutputSlots) output = true;
                    } else {
                        ItemStack stack = inventory.getStackInSlot(part, slot);
                        if (inventory.canExtractItem(part, slot, stack, side)) output = true;
                        else input = true;
                    }
                }
                int bit = 1 << side;
                if (isDistillationTower(targetTile)) {
                    boolean bottomItemPort = part.mDesign == 1 && part.getBlockPos().getY() == targetTile.getBlockPos().getY()
                        && side == CS.OPOS[facing];
                    output &= bottomItemPort;
                } else if (targetTile instanceof MultiTileEntityFermenter) {
                    output &= isFermenterOutputPort(part, targetTile, facing, side, false);
                }
                if ((mode & MultiTileEntityMultiBlockPart.NO_ITEM_IN) == 0 && input) itemInputSides |= bit;
                if ((mode & MultiTileEntityMultiBlockPart.NO_ITEM_OUT) == 0 && output) itemOutputSides |= bit;
            }
        }

        if (target instanceof IMultiBlockFluidHandler) {
            IMultiBlockFluidHandler fluids = (IMultiBlockFluidHandler) target;
            for (byte side = 0; side < 6; side++) {
                boolean input = false;
                boolean output = false;
                if (machine != null) {
                    byte rotatedSide = CS.FACING_ROTATIONS[facing][side];
                    input = CS.FACE_CONNECTED[rotatedSide][machine.mFluidInputs];
                    output = CS.FACE_CONNECTED[rotatedSide][machine.mFluidOutputs];
                } else if (targetTile instanceof MultiTileEntityTank
                    || targetTile instanceof MultiTileEntityCrucible
                    || targetTile instanceof MultiTileEntityLargeBoiler
                    || targetTile instanceof MultiTileEntityLargeHeatExchanger
                    || targetTile instanceof MultiTileEntityLargeTurbineGas
                    || targetTile instanceof MultiTileEntityLargeTurbineSteam
                    || targetTile instanceof MultiTileEntityBedrockDrill) {
                    gregapi.fluid.FluidTankInfo[] tanks = fluids.getTankInfo(part, side);
                    input = tanks != null && tanks.length > 0;
                    output = input;
                }
                int bit = 1 << side;
                if (machine != null && input && !machine.mDisabledFluidOutput
                    && outputSide(facing, machine.mFluidAutoOutput) == side) input = false;
                if (isDistillationTower(targetTile)) {
                    boolean fluidOutputPort = part.mDesign == 1 && part.getBlockPos().getY() > targetTile.getBlockPos().getY()
                        && side == CS.OPOS[facing];
                    output &= fluidOutputPort;
                } else if (targetTile instanceof MultiTileEntityLargeBoiler) {
                    output &= isLargeBoilerFluidOutputPort(part, targetTile, facing, side);
                } else if (targetTile instanceof MultiTileEntityFermenter) {
                    output &= isFermenterOutputPort(part, targetTile, facing, side, true);
                }
                if ((mode & MultiTileEntityMultiBlockPart.NO_FLUID_IN) == 0 && input) fluidInputSides |= bit;
                if ((mode & MultiTileEntityMultiBlockPart.NO_FLUID_OUT) == 0 && output) fluidOutputSides |= bit;
            }
        }

        CompoundTag energyInputTypes = new CompoundTag();
        CompoundTag energyOutputTypes = new CompoundTag();
        int energyOutputSides = writePartEnergyTypes(
            part,
            target,
            targetTile,
            facing,
            mode,
            energyInputTypes,
            energyOutputTypes);

        if (machine != null) {
            boolean distillationTower = isDistillationTower(targetTile);
            int itemAutoOutputSide = -1;
            if (CS.SIDES_VALID[machine.mItemAutoOutput]) {
                if (distillationTower || targetTile instanceof MultiTileEntityFermenter) {
                    itemAutoOutputSide = CS.OPOS[facing];
                }
            }
            if (hasSide(itemOutputSides, itemAutoOutputSide)) {
                int bit = 1 << itemAutoOutputSide;
                if (machine.mDisabledItemOutput) itemAutoOutputDisabledSides |= bit;
                else itemAutoOutputSides |= bit;
            }
            int fluidAutoSide = outputSide(facing, machine.mFluidAutoOutput);
            int fluidAutoOutputSide = -1;
            if (CS.SIDES_VALID[machine.mFluidAutoOutput]) {
                if (distillationTower || targetTile instanceof MultiTileEntityFermenter) {
                    fluidAutoOutputSide = CS.OPOS[facing];
                }
            }
            if (fluidAutoSide >= 0 && !machine.mDisabledFluidOutput) {
                fluidInputSides &= ~(1 << fluidAutoSide);
            }
            if (hasSide(fluidOutputSides, fluidAutoOutputSide)) {
                int bit = 1 << fluidAutoOutputSide;
                if (machine.mDisabledFluidOutput) fluidAutoOutputDisabledSides |= bit;
                else fluidAutoOutputSides |= bit;
            }
            int energyAutoSide = outputSide(facing, machine.mEnergyOutput);
            if (energyAutoSide >= 0) {
                int energyAutoOutputSide = adjacentPartSide(
                    part,
                    targetTile.getBlockPos().getX() + CS.OFFX[energyAutoSide],
                    targetTile.getBlockPos().getY() + CS.OFFY[energyAutoSide],
                    targetTile.getBlockPos().getZ() + CS.OFFZ[energyAutoSide]);
                if (hasSide(energyOutputSides, energyAutoOutputSide)) {
                    data.putInt(PART_ENERGY_AUTO_OUTPUT_SIDES, 1 << energyAutoOutputSide);
                }
            }
        } else if (target instanceof TileEntityBase11MultiBlockConverter) {
            TileEntityBase11MultiBlockConverter converter = (TileEntityBase11MultiBlockConverter) target;
            if (converter.getEmittingTileEntity() == part
                && hasSide(energyOutputSides, converter.getEmittingSide())) {
                data.putInt(PART_ENERGY_AUTO_OUTPUT_SIDES, 1 << converter.getEmittingSide());
            }
        }
        if (targetTile instanceof MultiTileEntityFusionReactor) {
            data.putInt(PART_ENERGY_AUTO_OUTPUT_SIDES, energyOutputSides);
        }

        data.putInt(PART_ITEM_INPUT_SIDES, itemInputSides);
        data.putInt(PART_ITEM_OUTPUT_SIDES, itemOutputSides);
        data.putInt(PART_FLUID_INPUT_SIDES, fluidInputSides);
        data.putInt(PART_FLUID_OUTPUT_SIDES, fluidOutputSides);
        data.putInt(PART_ITEM_AUTO_OUTPUT_SIDES, itemAutoOutputSides);
        data.putInt(PART_ITEM_AUTO_OUTPUT_DISABLED_SIDES, itemAutoOutputDisabledSides);
        data.putInt(PART_FLUID_AUTO_OUTPUT_SIDES, fluidAutoOutputSides);
        data.putInt(PART_FLUID_AUTO_OUTPUT_DISABLED_SIDES, fluidAutoOutputDisabledSides);
        data.put(PART_ENERGY_INPUT_TYPES, energyInputTypes);
        data.put(PART_ENERGY_OUTPUT_TYPES, energyOutputTypes);
    }

    private static int writePartEnergyTypes(MultiTileEntityMultiBlockPart part,
        ITileEntityMultiBlockController target, BlockEntity targetTile, byte facing, int mode,
        CompoundTag inputTypes, CompoundTag outputTypes) {
        if (!(target instanceof IMultiBlockEnergy)) return 0;
        IMultiBlockEnergy energy = (IMultiBlockEnergy) target;
        int inputForbidden = MultiTileEntityMultiBlockPart.NO_ENERGY_IN;
        int outputForbidden = MultiTileEntityMultiBlockPart.NO_ENERGY_OUT;
        int outputSides = 0;
        for (byte side = 0; side < 6; side++) {
            ListTag inputs = new ListTag();
            ListTag outputs = new ListTag();
            for (TagData type : energy.getEnergyTypes(part, side)) {
                if ((mode & inputForbidden) == 0 && energy.isEnergyAcceptingFrom(part, type, side, true)) {
                    inputs.add(StringTag.valueOf(type.getTranslatableNameShort()));
                }
                boolean fusionOutputPort = !(targetTile instanceof MultiTileEntityFusionReactor)
                    || isFusionEnergyOutputPort(part, targetTile, facing, side);
                if ((mode & outputForbidden) == 0 && fusionOutputPort
                    && energy.isEnergyEmittingTo(part, type, side, true)) {
                    outputs.add(StringTag.valueOf(type.getTranslatableNameShort()));
                }
            }
            String sideKey = Integer.toString(side);
            if (inputs.size() > 0) inputTypes.put(sideKey, inputs);
            if (outputs.size() > 0) {
                outputTypes.put(sideKey, outputs);
                outputSides |= 1 << side;
            }
        }
        return outputSides;
    }

    private static int outputSide(byte facing, byte localSide) {
        return CS.SIDES_VALID[localSide] ? CS.FACING_TO_SIDE[facing][localSide] : -1;
    }

    private static int adjacentPartSide(MultiTileEntityMultiBlockPart part, int x, int y, int z) {
        for (byte side = 0; side < 6; side++) {
            if (part.getBlockPos().getX() + CS.OFFX[side] == x && part.getBlockPos().getY() + CS.OFFY[side] == y
                && part.getBlockPos().getZ() + CS.OFFZ[side] == z) return side;
        }
        return -1;
    }

    private static boolean isLargeBoilerFluidOutputPort(MultiTileEntityMultiBlockPart part, BlockEntity boiler,
        byte facing, byte side) {
        if (part.mDesign != 1) return false;
        int centerX = boiler.getBlockPos().getX() - CS.OFFX[facing];
        int centerZ = boiler.getBlockPos().getZ() - CS.OFFZ[facing];
        int dx = part.getBlockPos().getX() - centerX;
        int dz = part.getBlockPos().getZ() - centerZ;
        if (part.getBlockPos().getY() == boiler.getBlockPos().getY() + 2) {
            return dx == 0 && dz == 0 && side == CS.SIDE_TOP;
        }
        if (part.getBlockPos().getY() != boiler.getBlockPos().getY() + 1) return false;
        return dx == -1 && dz == 0 && side == CS.SIDE_X_NEG
            || dx == 1 && dz == 0 && side == CS.SIDE_X_POS
            || dx == 0 && dz == -1 && side == CS.SIDE_Z_NEG
            || dx == 0 && dz == 1 && side == CS.SIDE_Z_POS;
    }

    private static boolean isFermenterOutputPort(MultiTileEntityMultiBlockPart part, BlockEntity fermenter,
        byte facing, byte side, boolean fluid) {
        int outputY = fermenter.getBlockPos().getY() + (fluid ? 0 : 1);
        return part.mDesign == 7 && part.getBlockPos().getY() == outputY && side == CS.OPOS[facing];
    }

    private static boolean isFusionEnergyOutputPort(MultiTileEntityMultiBlockPart part, BlockEntity reactor,
        byte facing, byte side) {
        if (part.mDesign != 2 || part.getBlockPos().getY() != reactor.getBlockPos().getY()) return false;
        int centerX = reactor.getBlockPos().getX() - CS.OFFX[facing] * 2;
        int centerZ = reactor.getBlockPos().getZ() - CS.OFFZ[facing] * 2;
        int dx = part.getBlockPos().getX() - centerX;
        int dz = part.getBlockPos().getZ() - centerZ;
        return dx == -9 && dz == 0 && side == CS.SIDE_X_NEG
            || dx == 9 && dz == 0 && side == CS.SIDE_X_POS
            || dx == 0 && dz == -9 && side == CS.SIDE_Z_NEG
            || dx == 0 && dz == 9 && side == CS.SIDE_Z_POS;
    }

    private static boolean isDistillationTower(BlockEntity target) {
        return target instanceof MultiTileEntityDistillationTower
            || target instanceof MultiTileEntityCryoDistillationTower;
    }

    private static boolean hasSide(int sideMask, int side) {
        return side >= 0 && (sideMask & (1 << side)) != 0;
    }

}
