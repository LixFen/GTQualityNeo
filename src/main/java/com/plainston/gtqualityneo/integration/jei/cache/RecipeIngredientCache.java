// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.cache;

import java.util.Map;
import java.util.WeakHashMap;

/** Cache extracted ingredients, not visibility decisions. Recipe changes invalidate only that recipe. */
public final class RecipeIngredientCache<K, V> {
    private record Entry<V>(long version, V value) {}
    private final Map<K, Entry<V>> entries = new WeakHashMap<>();
    private long resourceVersion = Long.MIN_VALUE;

    private void refresh(long resources) {
        if (resourceVersion != resources) {
            entries.clear();
            resourceVersion = resources;
        }
    }

    public synchronized V get(K recipe, long recipes, long resources) {
        refresh(resources);
        var entry = entries.get(recipe);
        return entry == null || entry.version() != recipes ? null : entry.value();
    }

    public synchronized void put(K recipe, V value, long recipes, long resources) {
        refresh(resources);
        entries.put(recipe, new Entry<>(recipes, value));
    }
}
