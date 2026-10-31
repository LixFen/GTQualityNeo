package com.plainston.gtqualityneo.client;

/** Machine inventory indexes include the reserved special slot even when no slot is exposed for it. */
public record MachineLayout(int inputItems, int outputItems, int inputFluids, int outputFluids, boolean special) {
    public static final int WIDTH = 176, TOP = 27, ARROW_X = 78, ARROW_WIDTH = 20, ARROW_HEIGHT = 18, GAP = 6;
    public record Position(int x, int y) {}

    private static int rows(int count) { return (count + 2) / 3; }
    int specialIndex() { return inputItems + outputItems; }
    int fluidStart() { return specialIndex() + 1; }
    private static int sideHeight(int items, int fluids) {
        return (rows(items) + rows(fluids)) * 18 + (items > 0 && fluids > 0 ? 4 : 0);
    }
    private int sideTop(boolean output) {
        int height = output ? sideHeight(outputItems, outputFluids) : sideHeight(inputItems, inputFluids);
        return TOP + (contentHeight() - height) / 2;
    }
    int fluidY(boolean output) {
        int items = output ? outputItems : inputItems;
        int fluids = output ? outputFluids : inputFluids;
        return sideTop(output) + rows(items) * 18 + (items > 0 && fluids > 0 ? 4 : 0);
    }
    public int contentHeight() {
        return Math.max(special ? 48 : 24,
            Math.max(sideHeight(inputItems, inputFluids), sideHeight(outputItems, outputFluids)));
    }
    public int arrowY() { return TOP + (contentHeight() - (special ? 42 : 18)) / 2; }
    int inventoryY() { return TOP + contentHeight() + 7; }
    int height() { return inventoryY() + 3 * 18 + 4 + 18 + 7; }
    int playerX(int index) { return 7 + index % 9 * 18; }
    int playerY(int index) {
        return index < 9 ? inventoryY() + 3 * 18 + 4 : inventoryY() + (index / 9 - 1) * 18;
    }

    public Position position(int index) {
        if (index < inputItems) return grid(index, inputItems, false, sideTop(false));
        if (index < specialIndex()) return grid(index - inputItems, outputItems, true, sideTop(true));
        if (index == specialIndex()) return new Position(79, arrowY() + 24);
        int fluid = index - fluidStart();
        return fluid < inputFluids ? grid(fluid, inputFluids, false, fluidY(false))
            : grid(fluid - inputFluids, outputFluids, true, fluidY(true));
    }

    private static Position grid(int index, int count, boolean output, int top) {
        int columns = Math.min(3, count - index / 3 * 3);
        int x = output ? ARROW_X + ARROW_WIDTH + GAP : ARROW_X - GAP - columns * 18;
        return new Position(x + index % 3 * 18, top + index / 3 * 18);
    }

}
