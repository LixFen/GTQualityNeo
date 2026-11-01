package com.plainston.gtqualityneo.client.render;

import gregapi.block.multitileentity.MultiTileEntityItemInternal;
import gregapi.block.multitileentity.MultiTileEntityClassContainer;
import gregapi.code.ItemNBT;
import gregapi.data.CS;
import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.tileentity.energy.TileEntityBase08Battery;
import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import java.util.IdentityHashMap;
import java.util.Map;

public final class BatteryBars {
    private static final IL[] BATTERIES = {
        IL.Battery_Lead_Acid_ULV, IL.Battery_Alkaline_ULV, IL.Battery_NiCd_ULV,
        IL.Battery_LiCoO2_ULV, IL.Battery_LiMn_ULV
    };
    private static final IL[] CELLS = {
        IL.Battery_Lead_Acid_Cell_Filled, IL.Battery_Alkaline_Cell_Filled, IL.Battery_NiCd_Cell_Filled,
        IL.Battery_LiCoO2_Cell_Filled, IL.Battery_LiMn_Cell_Filled
    };
    private static final Map<MultiTileEntityClassContainer, Integer> COLORS = new IdentityHashMap<>();

    private BatteryBars() {}

    public static void draw(net.minecraft.client.gui.GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (!com.plainston.gtqualityneo.GTQualityNeo.TOOL_BARS.get() || net.neoforged.fml.ModList.get().isLoaded("duradisplay")
            || stack.isEmpty() || !(stack.getItem() instanceof MultiTileEntityItemInternal)) return;
        MultiTileEntityItemInternal item = (MultiTileEntityItemInternal) stack.getItem();
        var registration = item.mBlock.mMultiTileEntityRegistry.getClassContainer(stack);
        if (registration == null || !(registration.mCanonicalTileEntity instanceof TileEntityBase08Battery battery)) return;
        var data = ItemNBT.get(stack);
        long capacity = capacity(data, battery.mCapacity);
        if (capacity <= 0) return;
        boolean charged = registration.mParameters.getBooleanOr(CS.NBT_ACTIVE_ENERGY, false);
        long energy = energy(data, capacity, battery.mEnergy, charged);
        int fallback = data == null ? battery.getPaint() : data.getIntOr(CS.NBT_COLOR, battery.getPaint());
        int color = COLORS.computeIfAbsent(registration, ignored -> color(stack));
        if (color < 0) color = fallback;
        // Batteries have no durability bar: their only bar belongs on the bottom row.
        ToolBars.gradientBar(graphics, x, y + 13, width(energy, capacity),
            shade(color, false), shade(color, true));
    }

    static long capacity(CompoundTag data, long fallback) {
        return data == null ? fallback : data.getLongOr(CS.NBT_CAPACITY, fallback);
    }

    static long energy(CompoundTag data, long capacity, long fallback, boolean charged) {
        if (data != null) charged = data.getBooleanOr(CS.NBT_ACTIVE_ENERGY, charged);
        return charged ? capacity : data == null ? fallback : data.getLongOr(CS.NBT_ENERGY, fallback);
    }

    private static int color(ItemStack stack) {
        for (int i = 0; i < BATTERIES.length; i++) {
            ItemStack base = BATTERIES[i].get(1);
            if (base.isEmpty() || stack.getItem() != base.getItem()) continue;
            int tier = ST.meta_(stack) - ST.meta_(base);
            // Each chemical battery series registers its five tiers consecutively.
            if (tier < 0 || tier > 4) continue;
            var fluid = FL.getFluid(CELLS[i].get(1), false);
            if (fluid == null || fluid.isEmpty()) return -1;
            var gtFluid = gregapi.fluid.FluidGT.of(fluid.getFluid());
            if (gtFluid != null) return gregapi.util.UT.Code.getRGBInt(gtFluid.getRGBa());
            return -1;
        }
        return -1;
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

