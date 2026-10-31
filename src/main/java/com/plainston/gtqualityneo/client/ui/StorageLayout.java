package com.plainston.gtqualityneo.client.ui;

/** Coordinates are slot borders; vanilla Slot coordinates refer to the item one pixel inside. */
record StorageLayout(int slots) {
    static final int WIDTH = 176;
    static final int PITCH = 18;
    static final int PANEL_Y = 18;
    static final int STORAGE_Y = PANEL_Y + 7;

    int rows() { return (slots + 8) / 9; }
    int inventoryY() { return STORAGE_Y + rows() * PITCH + 4; }
    int height() { return inventoryY() + 3 * PITCH + 4 + PITCH + 7; }
    int storageX(int index) {
        int columns = Math.min(9, slots);
        return (WIDTH - columns * PITCH) / 2 + index % 9 * PITCH;
    }
    int storageY(int index) { return STORAGE_Y + index / 9 * PITCH; }
    int playerX(int inventoryIndex) { return 7 + inventoryIndex % 9 * PITCH; }
    int playerY(int inventoryIndex) {
        return inventoryIndex < 9 ? inventoryY() + 3 * PITCH + 4
            : inventoryY() + (inventoryIndex / 9 - 1) * PITCH;
    }
}
