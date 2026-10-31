package com.plainston.gtqualityneo.client;

/** Pixel crop of GT6's 20x18 progress overlay, using its original quantization and animation cycles. */
public final class MachineProgress {
    public record Frame(int x, int y, int width, int height) {}

    private MachineProgress() {}

    public static Frame frame(int syncedValue, int direction, int cycles) {
        if (syncedValue < 0 || direction < 0 || direction > 7) return new Frame(0, 0, 0, 0);
        boolean horizontal = direction % 4 < 2;
        int size = horizontal ? 20 : 18;
        int scale = size * cycles;
        // Same step rounding as UT.Code.scale(value, 0, Short.MAX_VALUE, scale, false).
        int scaled = syncedValue <= 0 ? 0 : syncedValue >= Short.MAX_VALUE ? scale
            : scale <= 2 ? 1 : 1 + (int) ((long) syncedValue * (scale - 1) / Short.MAX_VALUE);
        int progress = scaled % (size + 1);
        if (direction >= 4) progress = size - progress;
        int x = horizontal && direction % 2 == 1 ? 20 - progress : 0;
        int y = !horizontal && direction % 2 == 1 ? 18 - progress : 0;
        return new Frame(x, y, horizontal ? progress : 20, horizontal ? 18 : progress);
    }
}
