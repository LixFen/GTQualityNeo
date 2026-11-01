package com.plainston.gtqualityneo.scanner;

import com.mojang.serialization.JavaOps;
import gregapi.block.prefixblock.PrefixBlockOreMap;

/** Read-only snapshot through GT6's public codec; no reflection or change to the GT6 dependency. */
final class PrefixOreIndex {
    static long[] snapshot(PrefixBlockOreMap map) {
        if (map == null || map.isEmpty()) return new long[0];
        // GT6 exposes get(x,y,z), but no iterator. Its codec enumerates exactly the stored entries.
        var encoded = PrefixBlockOreMap.CODEC.codec().encodeStart(JavaOps.INSTANCE, map).getOrThrow();
        var entries = JavaOps.INSTANCE.getMap(encoded).getOrThrow().get("gt6_ore");
        return decodeEntries(entries);
    }
    static long[] decodeEntries(Object entries) {
        return JavaOps.INSTANCE.getStream(entries).getOrThrow()
                .mapToLong(entry -> JavaOps.INSTANCE.getNumberValue(entry).getOrThrow().longValue()).toArray();
    }
    static int x(long entry) { return (int) (entry >>> 16) & 15; }
    static int z(long entry) { return ((int) (entry >>> 16) >>> 4) & 15; }
    static int y(long entry) { return ((int) (entry >>> 16) >>> 8) - 2048; }
    static short material(long entry) { return (short) entry; }
    private PrefixOreIndex() {}
}
