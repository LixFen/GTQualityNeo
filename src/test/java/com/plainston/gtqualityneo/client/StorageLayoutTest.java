package com.plainston.gtqualityneo.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class StorageLayoutTest {
    @Test void everySlotFitsWithoutOverlapForSmallHoppersAndFullChests() {
        for (int count = 1; count <= 108; count++) {
            var layout = new StorageLayout(count);
            var positions = new HashSet<String>();
            for (int i = 0; i < count; i++) {
                int x = layout.storageX(i), y = layout.storageY(i);
                assertTrue(x >= 0 && x + 18 <= StorageLayout.WIDTH, "count=" + count);
                assertTrue(y >= StorageLayout.PANEL_Y + 7 && y + 18 <= layout.inventoryY() - 4);
                assertTrue(positions.add(x + "," + y));
            }
            for (int i = 0; i < 36; i++) {
                int x = layout.playerX(i), y = layout.playerY(i);
                assertTrue(x >= 0 && x + 18 <= StorageLayout.WIDTH);
                assertTrue(y >= layout.inventoryY() && y + 18 < layout.height());
                assertTrue(positions.add(x + "," + y));
            }
        }
    }

    @Test void centersSmallInventoriesAndKeepsTheHotbarBelowTheMainInventory() {
        for (int count = 1; count <= 9; count++) {
            var layout = new StorageLayout(count);
            assertEquals(layout.storageX(0), StorageLayout.WIDTH - layout.storageX(count - 1) - 18);
            assertEquals(layout.playerY(27) + 22, layout.playerY(0));
        }
        assertEquals(1, new StorageLayout(9).rows());
        assertEquals(2, new StorageLayout(10).rows());
        assertEquals(6, new StorageLayout(54).rows());
        var chest = new StorageLayout(54);
        assertEquals(176, StorageLayout.WIDTH);
        assertEquals(7, chest.storageX(0));
        assertEquals(7, StorageLayout.WIDTH - chest.storageX(8) - 18);
        assertEquals(chest.storageX(0), chest.playerX(9));
        assertEquals(7, chest.height() - chest.playerY(0) - 18);
    }
}
