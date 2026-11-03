package com.plainston.gtqualityneo.client.ui;

import gregapi.gui.ContainerCommon;
import java.util.List;

/** Snapshot by menu order: distinct crafting controls can share the same inventory index. */
record FixedInventoryLayout(List<Position> sources) {
    static final int WIDTH = 176, TITLE_HEIGHT = 22;
    record Position(int x, int y) {}

    FixedInventoryLayout(ContainerCommon menu) {
        this(menu.slots.stream().map(slot -> new Position(slot.x - 1, slot.y - 1)).toList());
    }

    Position position(int menuIndex) {
        var source = sources.get(menuIndex);
        return new Position(source.x(), source.y() + TITLE_HEIGHT);
    }

    int height() {
        return sources.stream().mapToInt(Position::y).max().orElse(0) + TITLE_HEIGHT + 18 + 7;
    }
}
