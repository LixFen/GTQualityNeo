package com.plainston.gtqualityneo.scanner;

import java.util.*;

/** Full-layer counts plus the highest eight distinct resources per column, ordered by height. */
public final class ColumnData {
    public record Hit(String key, int y) {}
    public static final int LIMIT = 8;
    private final List<Hit> hits = new ArrayList<>();
    public void record(String key, int y) {
        for (int i = 0; i < hits.size(); i++) {
            if (hits.get(i).key().equals(key)) {
                if (hits.get(i).y() >= y) return;
                hits.remove(i);
                break;
            }
        }
        hits.add(new Hit(key, y));
        hits.sort(Comparator.comparingInt(Hit::y).reversed());
        if (hits.size() > LIMIT) hits.removeLast();
    }
    public List<Hit> hits() { return List.copyOf(hits); }
    public Hit firstMatching(Set<String> filter) {
        return hits.stream().filter(h -> filter.isEmpty() || filter.contains(h.key())).findFirst().orElse(null);
    }
}
