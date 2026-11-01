package com.plainston.gtqualityneo.scanner.client;

import com.plainston.gtqualityneo.scanner.ScanGrid;
import com.plainston.gtqualityneo.scanner.ScanNetwork;
import java.util.*;
import java.util.function.ToIntFunction;

/** CPU map cache. A streamed chunk changes 256 pixels; filters recolour the existing chunks once. */
public final class ScanMapRaster {
    private ScanGrid grid;
    private ScanNetwork.ChunkData[] applied = new ScanNetwork.ChunkData[0];
    private Set<String> filter = Set.of();
    private boolean terrain;
    private final Map<String, Integer> colours = new HashMap<>();
    private int side;
    private int[] pixels = new int[0], heights = new int[0];
    private String[] keys = new String[0];
    private int[] dirtyChunks = new int[0];
    private int dirtyCount;
    private boolean fullUpdate;
    public int side() { return side; }
    public int[] pixels() { return pixels; }
    public String key(int x, int z) { return keys[z * side + x]; }
    public int height(int x, int z) { return heights[z * side + x]; }
    public boolean fullUpdate() { return fullUpdate; }
    public int dirtyCount() { return dirtyCount; }
    public int dirtyChunk(int index) { return dirtyChunks[index]; }

    /** Returns the number of pixels updated, zero when no texture upload is needed. */
    public int update(ScanGrid nextGrid, Collection<ScanNetwork.ChunkData> chunks, Set<String> nextFilter,
            boolean nextTerrain, ToIntFunction<ScanNetwork.Resource> colour) {
        boolean reset = grid != nextGrid;
        if (reset) {
            grid = nextGrid; side = grid == null ? 0 : grid.width() * 16;
            applied = new ScanNetwork.ChunkData[grid == null ? 0 : grid.width() * grid.width()];
            dirtyChunks = new int[applied.length];
            pixels = new int[side * side]; heights = new int[pixels.length]; keys = new String[pixels.length];
            Arrays.fill(pixels, 0xff182433); colours.clear();
        }
        boolean recolour = reset || terrain != nextTerrain || !filter.equals(nextFilter);
        dirtyCount = 0;
        fullUpdate = recolour;
        if (recolour) { filter = Set.copyOf(nextFilter); terrain = nextTerrain; }
        int changed = reset ? pixels.length : 0;
        for (var chunk : chunks) {
            if (!recolour && applied[chunk.index()] == chunk) continue;
            dirtyChunks[dirtyCount++] = chunk.index();
            applied[chunk.index()] = chunk;
            int cx = chunk.index() / grid.width() * 16, cz = chunk.index() % grid.width() * 16;
            // Palette colour lookup (especially fluid models) occurs once per resource, never per pixel.
            int[] palette = new int[chunk.palette().size()];
            for (int i = 0; i < palette.length; i++) {
                var r = chunk.palette().get(i);
                palette[i] = colours.computeIfAbsent(r.key(), key -> colour.applyAsInt(r));
            }
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int local = x * 16 + z, target = (cz + z) * side + cx + x;
                keys[target] = null; heights[target] = 0;
                if (!chunk.available()) { pixels[target] = ((x + z) % 8 < 2) ? 0xff38414d : 0xff151a22; continue; }
                pixels[target] = terrain ? 0xff000000 | ((chunk.surface()[local] & 0xfefefe) >> 1) : 0xff0b1420;
                for (var cell : chunk.columns().get(local)) {
                    var r = chunk.palette().get(cell.resource());
                    if (!filter.isEmpty() && !filter.contains(r.key())) continue;
                    pixels[target] = palette[cell.resource()]; keys[target] = r.key(); heights[target] = cell.y(); break;
                }
            }
            if (!reset) changed += 256;
        }
        return changed;
    }
}
