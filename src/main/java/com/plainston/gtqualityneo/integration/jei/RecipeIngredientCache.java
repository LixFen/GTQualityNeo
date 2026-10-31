package com.plainston.gtqualityneo.integration.jei;

import java.util.Map;
import java.util.WeakHashMap;

/** Cache extracted ingredients, not visibility decisions. Discard old recipe/resource versions together. */
public final class RecipeIngredientCache<K, V> {
    private final Map<K, V> entries = new WeakHashMap<>();
    private long recipeVersion = Long.MIN_VALUE, resourceVersion = Long.MIN_VALUE;

    private void refresh(long recipes, long resources) {
        if (recipeVersion != recipes || resourceVersion != resources) {
            entries.clear();
            recipeVersion = recipes;
            resourceVersion = resources;
        }
    }

    public synchronized V get(K recipe, long recipes, long resources) {
        refresh(recipes, resources);
        return entries.get(recipe);
    }

    public synchronized void put(K recipe, V value, long recipes, long resources) {
        refresh(recipes, resources);
        entries.put(recipe, value);
    }
}
