package com.plainston.gtqualityneo.client.ui;

/** Coordinates are slot borders; vanilla Slot coordinates refer to the item one pixel inside. */
record StorageLayout(int slots, int columns) {
    static final int WIDTH = 176;
    static final int PITCH = 18;
    static final int PANEL_Y = 18;
    static final int STORAGE_Y = PANEL_Y + 7;

    int rows() { return (slots + columns - 1) / columns; }
    int inventoryY() { return STORAGE_Y + rows() * PITCH + 4; }
    int height() { return inventoryY() + 3 * PITCH + 4 + PITCH + 7; }
    int storageX(int index) {
        int rowStart = index / columns * columns;
        int rowSlots = Math.min(columns, slots - rowStart);
        return (WIDTH - rowSlots * PITCH) / 2 + (index - rowStart) * PITCH;
    }
    int storageY(int index) { return STORAGE_Y + index / columns * PITCH; }
    int playerX(int inventoryIndex) { return 7 + inventoryIndex % 9 * PITCH; }
    int playerY(int inventoryIndex) {
        return inventoryIndex < 9 ? inventoryY() + 3 * PITCH + 4
            : inventoryY() + (inventoryIndex / 9 - 1) * PITCH;
    }
}
