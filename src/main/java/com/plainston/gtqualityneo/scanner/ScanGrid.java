package com.plainston.gtqualityneo.scanner;

import java.util.Arrays;
import java.util.Comparator;

/** Pure coordinate math, shared by server and map. Width is a side length in chunks. */
public record ScanGrid(int chunkX, int chunkZ, int width) {
    public ScanGrid {
        if (width < 1 || width > 31) throw new IllegalArgumentException("Invalid scan width");
    }
    public static ScanGrid around(int blockX, int blockZ, int width) {
        int half = (width - 1) / 2;
        return new ScanGrid(Math.floorDiv(blockX, 16) - half, Math.floorDiv(blockZ, 16) - half, width);
    }
    public int x(int index) { return chunkX + index / width; }
    public int z(int index) { return chunkZ + index % width; }
    public int[] nearestFirst(int blockX, int blockZ) {
        int px = Math.floorDiv(blockX, 16), pz = Math.floorDiv(blockZ, 16);
        Integer[] order = new Integer[width * width];
        Arrays.setAll(order, i -> i);
        Arrays.sort(order, Comparator.comparingInt(i -> Math.max(Math.abs(x(i) - px), Math.abs(z(i) - pz))));
        return Arrays.stream(order).mapToInt(Integer::intValue).toArray();
    }
}
