package com.plainston.gtqualityneo.qol;

import com.plainston.gtqualityneo.GTQualityNeo;
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
        if (DrillInteraction.tier(stack) > 0 && serverEnabled(GTQualityNeo.DRILL_MODES)) return true;
        if (CircuitInteraction.isCircuit(stack) && serverEnabled(GTQualityNeo.CIRCUIT_SELECTOR)) return true;
        if (MoldInteraction.isChisel(stack) && (serverEnabled(GTQualityNeo.MOLD_SELECTOR)
            || serverEnabled(GTQualityNeo.STORAGE_FORM))) return true;
        if (UniversalHazmat.isUniversal(stack) && UniversalHazmat.enhanced()) return true;
        if (ArmorsGT.HAZMATS_HEAT.contains(stack, true) && serverEnabled(GTQualityNeo.HEAT_HAZMAT_IMMUNITY)) return true;
        if (stack.getItem() instanceof BlockItem block && block.getBlock() instanceof TrapDoorBlock) return true;
        var cover = CoverRegistry.get(stack);
        if ((cover instanceof AbstractCoverAttachmentTorch || cover instanceof CoverPressureValve)
            && serverEnabled(GTQualityNeo.HOPPER_SHAPES)) return true;
        if (!(stack.getItem() instanceof MultiTileEntityItemInternal item)) return false;
        var container = item.mBlock.mMultiTileEntityRegistry.getClassContainer(stack);
        if (container == null) return false;
        var tile = container.mCanonicalTileEntity;
        return tile.getClass() == MultiTileEntityMold.class && serverEnabled(GTQualityNeo.MOLD_SELECTOR)
            || tile instanceof MultiTileEntityPipeFluid && serverEnabled(GTQualityNeo.ONE_WAY_PIPES)
            || tile instanceof MultiTileEntityWireRedstone
            || tile instanceof MultiTileEntityWireRedstoneInsulated
            || tile instanceof MultiTileEntityMassStorage && serverEnabled(GTQualityNeo.STORAGE_FORM)
            || tile instanceof MultiTileEntitySapBag && serverEnabled(GTQualityNeo.SAP_BAG_EXTRACTION)
            || tile instanceof MultiTileEntityScaffold && scaffoldChangesEnabled()
            || tile instanceof MultiTileEntityHopper && serverEnabled(GTQualityNeo.HOPPER_SHAPES)
            || tile instanceof MultiTileEntityQueueHopper && serverEnabled(GTQualityNeo.HOPPER_SHAPES)
            || tile instanceof MultiTileEntityGeneratorSolid
            || tile instanceof MultiTileEntityGeneratorLiquid
            || tile instanceof MultiTileEntityGeneratorFluidBed
            || tile instanceof MultiTileEntityFilter && clientEnabled(GTQualityNeo.FILTER_GHOST)
            || tile instanceof MultiTileEntityAdvancedCraftingTable;
    }

    private static boolean serverEnabled(net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value) {
        return GTQualityNeo.SERVER_CONFIG.isLoaded() && value.get();
    }

    private static boolean clientEnabled(net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value) {
        return GTQualityNeo.CLIENT_CONFIG.isLoaded() && value.get();
    }

    private static boolean scaffoldChangesEnabled() {
        return serverEnabled(GTQualityNeo.SCAFFOLD_COLLISION)
            || GTQualityNeo.SERVER_CONFIG.isLoaded()
                && (GTQualityNeo.SCAFFOLD_UP.get() > 0 || GTQualityNeo.SCAFFOLD_DOWN.get() > 0);
    }
}
