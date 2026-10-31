package com.plainston.gtqualityneo.client;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MachineProgressTest {
    @Test void preservesGt6DirectionalAndInvertedCrops() {
        assertEquals(new MachineProgress.Frame(0, 0, 10, 18), MachineProgress.frame(16383, 0, 1));
        assertEquals(new MachineProgress.Frame(10, 0, 10, 18), MachineProgress.frame(16383, 1, 1));
        assertEquals(new MachineProgress.Frame(0, 0, 20, 9), MachineProgress.frame(16383, 2, 1));
        assertEquals(new MachineProgress.Frame(0, 9, 20, 9), MachineProgress.frame(16383, 3, 1));
        for (int direction = 0; direction < 8; direction++) {
            assertEquals(new MachineProgress.Frame(0, 0, 0, 0), MachineProgress.frame(-1, direction, 1));
            var initial = MachineProgress.frame(0, direction, 1);
            var completed = MachineProgress.frame(Short.MAX_VALUE, direction, 1);
            assertEquals(direction < 4 ? 0 : 360, initial.width() * initial.height());
            assertEquals(direction < 4 ? 360 : 0, completed.width() * completed.height());
        }
    }

    @Test void preservesAnimationCyclesAndNeverSamplesOutsideTheOriginalOverlay() {
        assertEquals(20, MachineProgress.frame(16383, 0, 2).width());
        assertEquals(19, MachineProgress.frame(Short.MAX_VALUE, 0, 2).width());
        assertEquals(18, MachineProgress.frame(Short.MAX_VALUE, 0, 3).width());
        for (int direction = 0; direction < 8; direction++) for (int cycles : new int[]{1, 2, 3, 255})
            for (int progress : new int[]{0, 1, 8192, 16383, 24576, 32767}) {
                var frame = MachineProgress.frame(progress, direction, cycles);
                assertTrue(frame.x() >= 0 && frame.y() >= 0);
                assertTrue(frame.width() >= 0 && frame.height() >= 0);
                assertTrue(frame.x() + frame.width() <= 20 && frame.y() + frame.height() <= 18);
            }
    }
}
