package com.plainston.gtqualityneo.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DrillModesTest {

    @Test
    void tiersCycleThroughOnlyTheirSupportedModes() {
        for (int tier = 1; tier <= 3; tier++) {
            int mode = 0;
            assertEquals(0, DrillModes.depth(tier, mode));
            mode = DrillModes.next(tier, mode);
            assertEquals(1, DrillModes.depth(tier, mode));
            mode = DrillModes.next(tier, mode);
            if (tier > 1) {
                assertEquals(tier, DrillModes.depth(tier, mode));
                mode = DrillModes.next(tier, mode);
            }
            assertEquals(0, mode);
        }
        assertEquals(0, DrillModes.normalize(1, 2));
        assertEquals(0, DrillModes.normalize(2, -1));
        assertEquals(0, DrillModes.normalize(3, Integer.MAX_VALUE));
        assertEquals(0, DrillModes.depth(0, 1));
    }

    @Test
    void allSixFacesHaveThreeByThreeLayersExtendingOnlyInwards() {
        for (int side = 0; side < 6; side++) {
            int normal = side < 2 ? 1 : side < 4 ? 2 : 0;
            int direction = side % 2 == 0 ? 1 : -1;
            for (int depth = 1; depth <= 3; depth++) {
                List<int[]> offsets = DrillModes.offsets(side, depth);
                assertEquals(9 * depth - 1, offsets.size());
                Set<String> unique = new HashSet<>();
                int[] counts = new int[depth];
                for (int[] offset : offsets) {
                    int layer = offset[normal] * direction;
                    assertTrue(layer >= 0 && layer < depth);
                    counts[layer]++;
                    for (int axis = 0; axis < 3; axis++) {
                        if (axis != normal) assertTrue(Math.abs(offset[axis]) <= 1);
                    }
                    assertFalse(offset[0] == 0 && offset[1] == 0 && offset[2] == 0);
                    assertTrue(unique.add(offset[0] + "," + offset[1] + "," + offset[2]));
                }
                assertEquals(8, counts[0]);
                for (int layer = 1; layer < depth; layer++) assertEquals(9, counts[layer]);
            }
        }
        assertTrue(
            DrillModes.offsets(0, 0)
                .isEmpty());
        assertTrue(
            DrillModes.offsets(-1, 1)
                .isEmpty());
    }
}
