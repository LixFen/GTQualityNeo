package com.plainston.gtqualityneo.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class MachineLayoutTest {
    @Test void itemFluidSpecialAndPlayerSlotsNeverOverlapForDifferentRecipeShapes() {
        int[] counts = {0, 1, 3, 4, 6, 9, 12};
        for (int in : counts) for (int out : counts) for (int fluidIn : counts) for (int fluidOut : counts)
            for (boolean special : new boolean[] {false, true}) {
                var layout = new MachineLayout(in, out, fluidIn, fluidOut, special);
                var positions = new ArrayList<MachineLayout.Position>();
                for (int index = 0; index < layout.fluidStart() + fluidIn + fluidOut; index++) {
                    if (index == layout.specialIndex() && !special) continue;
                    var position = layout.position(index);
                    assertTrue(position.x() >= 7 && position.x() + 18 <= MachineLayout.WIDTH - 7);
                    assertTrue(position.y() >= MachineLayout.TOP && position.y() + 18 <= layout.inventoryY() - 7);
                    assertFalse(overlaps(position, 18, 18, new MachineLayout.Position(MachineLayout.ARROW_X, layout.arrowY()),
                        MachineLayout.ARROW_WIDTH, MachineLayout.ARROW_HEIGHT));
                    positions.add(position);
                }
                for (int index = 0; index < 36; index++)
                    positions.add(new MachineLayout.Position(layout.playerX(index), layout.playerY(index)));
                for (int i = 0; i < positions.size(); i++) {
                    assertTrue(positions.get(i).y() + 18 <= layout.height() - 7);
                    for (int j = 0; j < i; j++)
                        assertFalse(overlaps(positions.get(i), 18, 18, positions.get(j), 18, 18));
                }
            }
    }

    private static boolean overlaps(MachineLayout.Position a, int aw, int ah,
                                    MachineLayout.Position b, int bw, int bh) {
        return a.x() < b.x() + bw && a.x() + aw > b.x() && a.y() < b.y() + bh && a.y() + ah > b.y();
    }

    @Test void fluidIndexesSkipTheReservedSpecialIndexAndOutputsAreOnTheRight() {
        var layout = new MachineLayout(2, 3, 4, 9, false);
        assertEquals(5, layout.specialIndex());
        assertEquals(6, layout.fluidStart());
        assertTrue(layout.position(6).x() < 70);
        assertTrue(layout.position(10).x() > 100);
        assertEquals(layout.fluidY(false), layout.position(6).y());
        assertTrue(layout.position(6).y() > layout.position(0).y() + 18);
    }

    @Test void smallGroupsHugTheCenterAndEachSideIsVerticallyCentered() {
        var layout = new MachineLayout(1, 9, 0, 0, false);
        assertEquals(MachineLayout.ARROW_X - MachineLayout.GAP, layout.position(0).x() + 18);
        assertEquals(MachineLayout.ARROW_X + MachineLayout.ARROW_WIDTH + MachineLayout.GAP, layout.position(1).x());
        assertEquals(MachineLayout.TOP + (layout.contentHeight() - 18) / 2, layout.position(0).y());
        assertEquals(MachineLayout.TOP, layout.position(1).y());
        var partial = new MachineLayout(5, 1, 0, 0, false);
        assertEquals(partial.position(2).x(), partial.position(4).x(), "Input rows align toward the center");
    }
}
