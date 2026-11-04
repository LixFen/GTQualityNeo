package com.plainston.gtqualityneo.integration.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class ReiPreviewGrid {
    private ReiPreviewGrid() {}

    public record Cell(EntryIngredient entries, boolean output) {}

    public static void add(List<Widget> widgets, List<Cell> cells, int x, int y, int columns) {
        int size = columns * 2;
        var slots = new ArrayList<Slot>();
        for (int i = 0; i < Math.min(size, cells.size()); i++) {
            var slot = Widgets.createSlot(new Point(x + i % columns * 18, y + i / columns * 18));
            slots.add(slot);
            widgets.add(slot);
        }
        populate(slots, cells, 0);
        if (cells.size() > size) {
            int pages = (cells.size() + size - 1) / size;
            int[] page = {0};
            widgets.add(Widgets.createButton(new Rectangle(x, y + 38, 20, 12), Component.literal("<"))
                .onClick(button -> populate(slots, cells, (page[0] = Math.floorMod(page[0] - 1, pages)) * size)));
            widgets.add(Widgets.createButton(new Rectangle(x + 24, y + 38, 20, 12), Component.literal(">"))
                .onClick(button -> populate(slots, cells, (page[0] = (page[0] + 1) % pages) * size)));
        }
    }

    private static void populate(List<Slot> slots, List<Cell> cells, int offset) {
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i).clearEntries().unmarkInputOrOutput();
            if (offset + i >= cells.size()) continue;
            var cell = cells.get(offset + i);
            slot.entries(cell.entries());
            if (cell.output()) slot.markOutput(); else slot.markInput();
        }
    }
}
