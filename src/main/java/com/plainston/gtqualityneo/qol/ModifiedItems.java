package com.plainston.gtqualityneo.qol;

import com.plainston.gtqualityneo.tools.DrillInteraction;
import gregapi.block.multitileentity.MultiTileEntityItemInternal;
import gregapi.cover.CoverRegistry;
import gregapi.cover.covers.AbstractCoverAttachmentTorch;
import gregapi.cover.covers.CoverPressureValve;
import gregapi.data.CS.ArmorsGT;
import gregapi.tileentity.connectors.MultiTileEntityPipeFluid;
import gregapi.tileentity.connectors.MultiTileEntityWireRedstone;
import gregapi.tileentity.connectors.MultiTileEntityWireRedstoneInsulated;
import gregapi.tileentity.inventories.MultiTileEntityMassStorage;
import gregapi.tileentity.tools.MultiTileEntityAdvancedCraftingTable;
import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorFluidBed;
import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorLiquid;
import gregtech.tileentity.energy.generators.MultiTileEntityGeneratorSolid;
import gregtech.tileentity.extenders.MultiTileEntityFilter;
import gregtech.tileentity.inventories.MultiTileEntityHopper;
import gregtech.tileentity.inventories.MultiTileEntityQueueHopper;
import gregtech.tileentity.tools.MultiTileEntityMold;
import gregtech.tileentity.tools.MultiTileEntitySapBag;
import gregtech.tileentity.tools.MultiTileEntityScaffold;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.TrapDoorBlock;

/** Targeted changes only: generic machine, tool, Jade and JEI integrations do not qualify. */
public final class ModifiedItems {
    private ModifiedItems() {}

    public static boolean contains(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (DrillInteraction.tier(stack) > 0 || CircuitInteraction.isCircuit(stack)
            || MoldInteraction.isChisel(stack) || UniversalHazmat.isUniversal(stack)
            || ArmorsGT.HAZMATS_HEAT.contains(stack, true)) return true;
        if (stack.getItem() instanceof BlockItem block && block.getBlock() instanceof TrapDoorBlock) return true;
        var cover = CoverRegistry.get(stack);
        if (cover instanceof AbstractCoverAttachmentTorch || cover instanceof CoverPressureValve) return true;
        if (!(stack.getItem() instanceof MultiTileEntityItemInternal item)) return false;
        var container = item.mBlock.mMultiTileEntityRegistry.getClassContainer(stack);
        if (container == null) return false;
        var tile = container.mCanonicalTileEntity;
        return tile.getClass() == MultiTileEntityMold.class
            || tile instanceof MultiTileEntityPipeFluid
            || tile instanceof MultiTileEntityWireRedstone
            || tile instanceof MultiTileEntityWireRedstoneInsulated
            || tile instanceof MultiTileEntityMassStorage
            || tile instanceof MultiTileEntitySapBag
            || tile instanceof MultiTileEntityScaffold
            || tile instanceof MultiTileEntityHopper
            || tile instanceof MultiTileEntityQueueHopper
            || tile instanceof MultiTileEntityGeneratorSolid
            || tile instanceof MultiTileEntityGeneratorLiquid
            || tile instanceof MultiTileEntityGeneratorFluidBed
            || tile instanceof MultiTileEntityFilter
            || tile instanceof MultiTileEntityAdvancedCraftingTable;
    }
}
