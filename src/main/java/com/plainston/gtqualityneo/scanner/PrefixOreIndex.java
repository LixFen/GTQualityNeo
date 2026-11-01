package com.plainston.gtqualityneo.scanner;

import com.plainston.gtqualityneo.mixin.PrefixOreMapAccessor;
import gregapi.block.prefixblock.PrefixBlockOreMap;
import it.unimi.dsi.fastutil.ints.Int2ShortMaps;

/** Primitive snapshot: no codec, boxed longs or live iterator retained across server ticks. */
final class PrefixOreIndex {
    static long[] snapshot(PrefixBlockOreMap map) {
        if (map == null || map.isEmpty()) return new long[0];
        long[] result = new long[map.size()];
        var entries = Int2ShortMaps.fastIterator(((PrefixOreMapAccessor) (Object) map).gtqualityneo$entries());
        int index = 0;
        while (entries.hasNext()) {
            var entry = entries.next();
            result[index++] = ((long) entry.getIntKey() << 16) | (entry.getShortValue() & 0xffffL);
        }
        return result;
    }
    static int x(long entry) { return (int) (entry >>> 16) & 15; }
    static int z(long entry) { return ((int) (entry >>> 16) >>> 4) & 15; }
    static int y(long entry) { return ((int) (entry >>> 16) >>> 8) - 2048; }
    static short material(long entry) { return (short) entry; }
    private PrefixOreIndex() {}
}
