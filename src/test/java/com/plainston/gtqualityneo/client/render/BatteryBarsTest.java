package com.plainston.gtqualityneo.client.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BatteryBarsTest {
    @Test
    void chargeWidthHandlesEmptyPartialFullAndInvalidValues() {
        assertEquals(0, BatteryBars.width(0, 100));
        assertEquals(1, BatteryBars.width(8, 100));
        assertEquals(7, BatteryBars.width(50, 100));
        assertEquals(13, BatteryBars.width(100, 100));
        assertEquals(13, BatteryBars.width(150, 100));
        assertEquals(0, BatteryBars.width(-5, 100));
        assertEquals(0, BatteryBars.width(10, 0));
        assertEquals(7, BatteryBars.width(Long.MAX_VALUE / 2, Long.MAX_VALUE));
    }

    @Test
    void gradientKeepsTheFluidHueAndIgnoresAlpha() {
        assertEquals(0x003f7f, BatteryBars.shade(0xff007fff, false));
        assertEquals(0x55a9ff, BatteryBars.shade(0xff007fff, true));
        assertEquals(0x000000, BatteryBars.shade(0x000000, false));
        assertEquals(0x555555, BatteryBars.shade(0x000000, true));
        assertEquals(0x7f7f7f, BatteryBars.shade(0xffffff, false));
        assertEquals(0xffffff, BatteryBars.shade(0xffffff, true));
    }
}

