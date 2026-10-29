package com.plainston.gtqualityneo.client;

import gregapi.block.multitileentity.MultiTileEntityItemInternal;
import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.tileentity.energy.TileEntityBase08Battery;
import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;

public final class BatteryBars {
    private static final IL[] BATTERIES = {
        IL.Battery_Lead_Acid_ULV, IL.Battery_Alkaline_ULV, IL.Battery_NiCd_ULV,
        IL.Battery_LiCoO2_ULV, IL.Battery_LiMn_ULV
    };
    private static final IL[] CELLS = {
        IL.Battery_Lead_Acid_Cell_Filled, IL.Battery_Alkaline_Cell_Filled, IL.Battery_NiCd_Cell_Filled,
        IL.Battery_LiCoO2_Cell_Filled, IL.Battery_LiMn_Cell_Filled
    };

    private BatteryBars() {}

    public static void draw(net.minecraft.client.gui.GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (!com.plainston.gtqualityneo.GTQualityNeo.TOOL_BARS.get() || net.neoforged.fml.ModList.get().isLoaded("duradisplay")
            || stack.isEmpty() || !(stack.getItem() instanceof MultiTileEntityItemInternal)) return;
        MultiTileEntityItemInternal item = (MultiTileEntityItemInternal) stack.getItem();
        var registration = item.mBlock.mMultiTileEntityRegistry.getClassContainer(stack);
        if (registration == null || !TileEntityBase08Battery.class.isAssignableFrom(registration.mClass)) return;
        var container = item.mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(stack);
        if (container == null || !(container.mTileEntity instanceof TileEntityBase08Battery)) return;
        TileEntityBase08Battery battery = (TileEntityBase08Battery) container.mTileEntity;
        if (battery.mCapacity <= 0) return;
        int color = color(stack, battery.getPaint());
        // Batteries have no durability bar: their only bar belongs on the bottom row.
        ToolBars.gradientBar(graphics, x, y + 13, width(battery.mEnergy, battery.mCapacity),
            shade(color, false), shade(color, true));
    }

    private static int color(ItemStack stack, int fallback) {
        for (int i = 0; i < BATTERIES.length; i++) {
            ItemStack base = BATTERIES[i].get(1);
            if (base.isEmpty() || stack.getItem() != base.getItem()) continue;
            int tier = ST.meta_(stack) - ST.meta_(base);
            // Each chemical battery series registers its five tiers consecutively.
            if (tier < 0 || tier > 4) continue;
            var fluid = FL.getFluid(CELLS[i].get(1), false);
            if (fluid == null || fluid.isEmpty()) return fallback;
            var gtFluid = gregapi.fluid.FluidGT.of(fluid.getFluid());
            if (gtFluid != null) return gregapi.util.UT.Code.getRGBInt(gtFluid.getRGBa());
            return fallback;
        }
        return fallback;
    }

    public static int width(long energy, long capacity) {
        return capacity <= 0 ? 0 : (int) Math.round(13.0 * Math.max(0, Math.min(energy, capacity)) / capacity);
    }

    public static int shade(int color, boolean light) {
        int result = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            int channel = color >> shift & 255;
            result |= (light ? channel + (255 - channel) / 3 : channel / 2) << shift;
        }
        return result;
    }
}

