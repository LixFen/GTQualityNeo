package com.plainston.gtqualityneo.client;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.qol.ModifiedItems;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ModifiedTooltip {
    private static final int[] COLORS = {0xff6666, 0xffcc66, 0xffff66, 0x66ff99, 0x66ccff, 0xcc88ff};
    private ModifiedTooltip() {}

    public static void append(ItemTooltipEvent event) {
        if (!GTQualityNeo.CLIENT_CONFIG.isLoaded() || !GTQualityNeo.MODIFIED_TOOLTIPS.get()
            || !ModifiedItems.contains(event.getItemStack())) return;
        String text = Component.translatable("gtqualityneo.tooltip.modified").getString();
        var line = Component.empty();
        double phase = (System.nanoTime() / 1_000_000L % 6000) / 1000.0;
        int[] characters = text.codePoints().toArray();
        for (int i = 0; i < characters.length; i++) {
            double position = (phase + i * 0.2) % COLORS.length;
            int index = (int) position;
            float fraction = (float) (position - index);
            int color = 0;
            for (int shift : new int[]{0, 8, 16}) {
                int from = COLORS[index] >> shift & 255;
                int to = COLORS[(index + 1) % COLORS.length] >> shift & 255;
                color |= Math.round(from + (to - from) * fraction) << shift;
            }
            line.append(Component.literal(new String(Character.toChars(characters[i]))).withColor(color));
        }
        event.getToolTip().add(line);
    }
}
