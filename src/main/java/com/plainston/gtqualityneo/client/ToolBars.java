package com.plainston.gtqualityneo.client;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.data.TD;
import gregapi.item.multiitem.MultiItemTool;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class ToolBars {
    private ToolBars() {}
    public static boolean enabled(ItemStack stack) {
        return stack.getItem() instanceof MultiItemTool && GTQualityNeo.TOOL_BARS.get() && !ModList.get().isLoaded("duradisplay");
    }
    public static void draw(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (!enabled(stack)) return;
        var data = gregapi.code.ItemNBT.get(stack);
        if (data != null && data.getBooleanOr("gtquality.hideHarvestIconDurability", false)) return;
        MultiItemTool tool = (MultiItemTool) stack.getItem();
        long maximum = MultiItemTool.getToolMaxDamage(stack);
        if (maximum > 0) bar(graphics, x, y + 13, width(Math.max(0, maximum - MultiItemTool.getToolDamage(stack)), maximum), 0x147c00, 0x73ff59);
        var energy = tool.getEnergyStats(stack);
        if (energy != null) {
            long capacity = energy.getEnergyCapacity(TD.Energy.EU, stack), charge = energy.getEnergyStored(TD.Energy.EU, stack);
            if (capacity > 0 && charge > 0) bar(graphics, x, y + (maximum > 0 ? 11 : 13), width(charge, capacity), 0x0065b2, 0xd9eeff);
        }
    }
    public static int width(long remaining, long maximum) {
        return maximum <= 0 ? 0 : (int) Math.round(13.0 * Math.max(0, Math.min(remaining, maximum)) / maximum);
    }
    private static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, int left, int right) {
        if (width <= 3) { left = 0x7a0000; right = 0xff1b1b; }
        graphics.fill(x + 2, y, x + 15, y + 2, 0xff000000);
        for (int i = 0; i < width; i++) {
            float t = width <= 1 ? 0 : (float) i / (width - 1);
            int color = 0xff000000;
            for (int shift : new int[]{0, 8, 16}) color |= Math.round((left >> shift & 255) * (1 - t) + (right >> shift & 255) * t) << shift;
            graphics.fill(x + 2 + i, y, x + 3 + i, y + 1, color);
        }
    }
}
