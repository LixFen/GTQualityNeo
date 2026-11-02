// 兼容说明：候选键须宽于 GT6w alpha.17 getNEIRecipes/getNEIUsages 的匹配（同 item 且 meta 相等/W/可损坏；流体按 id）；上游放宽匹配后需复核。
package com.plainston.gtqualityneo.integration.jei.machine;

import gregapi.data.CS;
import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.recipes.Recipe;
import gregapi.util.ST;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Display-only input/output index narrowing GT6's NEI scan. Never changes GT6's machine recipe selection. */
public final class RecipeQueryIndex {
    public interface Holder { RecipeQueryIndex gtqualityneo$queryIndex(); }
    private static final ThreadLocal<Capture> CAPTURE = new ThreadLocal<>();
    /** Records the expanded focuses actually scanned by GT6, including redirects and fluid containers. */
    public static final class Capture implements AutoCloseable {
        private final Capture previous = CAPTURE.get();
        private final List<QueryStamp> queries = new ArrayList<>();
        private Capture() { CAPTURE.set(this); }
        public List<QueryStamp> queries() { return List.copyOf(queries); }
        @Override public void close() {
            if (previous == null) CAPTURE.remove(); else CAPTURE.set(previous);
        }
    }
    public static Capture capture() { return new Capture(); }
    public static final class QueryStamp {
        private final RecipeQueryIndex index;
        private final Keys keys;
        private final boolean input;
        private final long version, generation;
        private QueryStamp(RecipeQueryIndex index, Keys keys, boolean input) {
            this.index = index; this.keys = keys; this.input = input;
            version = index.version(keys, input); generation = index.generation;
        }
        public boolean valid() {
            synchronized (index) {
                return generation == index.generation && version == index.version(keys, input);
            }
        }
    }
    private record ItemKey(Item item, short meta) {}
    private record Keys(Set<ItemKey> items, Set<Integer> fluids) {}
    private record Entry(long order, Keys inputs, Keys outputs) {}
    private final Map<Recipe, Entry> members = new IdentityHashMap<>();
    private final Side inputs = new Side(), outputs = new Side();
    private long sequence;
    private long generation;

    private static final class Side {
        final Map<ItemKey, Set<Recipe>> items = new HashMap<>();
        final Map<Integer, Set<Recipe>> fluids = new HashMap<>();
        // Keep versions even for emptied buckets: removing the last match must invalidate its queries.
        final Map<ItemKey, Long> itemVersions = new HashMap<>();
        final Map<Integer, Long> fluidVersions = new HashMap<>();
        void changed(Keys keys) {
            for (var key : keys.items()) itemVersions.merge(key, 1L, Long::sum);
            for (var key : keys.fluids()) fluidVersions.merge(key, 1L, Long::sum);
        }
        void add(Recipe recipe, Keys keys) {
            for (var key : keys.items()) items.computeIfAbsent(key, unused -> new LinkedHashSet<>()).add(recipe);
            for (int key : keys.fluids()) fluids.computeIfAbsent(key, unused -> new LinkedHashSet<>()).add(recipe);
            changed(keys);
        }
        void remove(Recipe recipe, Keys keys) {
            for (var key : keys.items()) remove(items, key, recipe);
            for (int key : keys.fluids()) remove(fluids, key, recipe);
            changed(keys);
        }
        private static <K> void remove(Map<K, Set<Recipe>> index, K key, Recipe recipe) {
            var bucket = index.get(key);
            bucket.remove(recipe);
            if (bucket.isEmpty()) index.remove(key);
        }
    }

    private static Keys keys(ItemStack[] items, FluidStack[] fluids) {
        var itemKeys = new LinkedHashSet<ItemKey>();
        var fluidKeys = new LinkedHashSet<Integer>();
        for (var stack : items) if (stack != null)
            itemKeys.add(new ItemKey(stack.getItem(), stack.isDamageableItem() ? CS.W : ST.meta_(stack)));
        for (var fluid : fluids) if (fluid != null) fluidKeys.add((int) FL.id(fluid));
        return new Keys(itemKeys, fluidKeys);
    }

    public synchronized void add(Recipe recipe) {
        if (members.containsKey(recipe)) return;
        var entry = new Entry(sequence++, keys(recipe.mInputs, recipe.mFluidInputs), keys(recipe.mOutputs, recipe.mFluidOutputs));
        members.put(recipe, entry);
        inputs.add(recipe, entry.inputs()); outputs.add(recipe, entry.outputs());
    }
    public synchronized void remove(Recipe recipe) {
        var entry = members.remove(recipe);
        if (entry == null) return;
        inputs.remove(recipe, entry.inputs()); outputs.remove(recipe, entry.outputs());
    }
    public synchronized void rebuild(Collection<Recipe> recipes) {
        members.clear(); inputs.items.clear(); inputs.fluids.clear(); outputs.items.clear(); outputs.fluids.clear(); sequence = 0;
        inputs.itemVersions.clear(); inputs.fluidVersions.clear(); outputs.itemVersions.clear(); outputs.fluidVersions.clear();
        generation++;
        for (var recipe : recipes) add(recipe);
    }
    public synchronized int size() { return members.size(); }

    /** Recipes GT6's own match loop needs to see; GT6 still applies the exact match and the enabled/hidden filter. */
    public synchronized List<Recipe> candidates(ItemStack[] focuses, boolean input) {
        var side = input ? inputs : outputs;
        var candidates = new LinkedHashSet<Recipe>();
        var itemKeys = new LinkedHashSet<ItemKey>();
        var fluidKeys = new LinkedHashSet<Integer>();
        for (var stack : focuses) {
            if (stack == null) continue;
            int meta = ST.meta_(stack);
            if (IL.Display_Fluid.equal(stack, true, true)) {
                if (meta >= 0) fluidKeys.add(meta);
            } else {
                itemKeys.add(new ItemKey(stack.getItem(), (short) meta));
                itemKeys.add(new ItemKey(stack.getItem(), CS.W));
            }
        }
        for (var key : itemKeys) collect(candidates, side.items.get(key));
        for (int key : fluidKeys) collect(candidates, side.fluids.get(key));
        var capture = CAPTURE.get();
        if (capture != null) capture.queries.add(new QueryStamp(this, new Keys(itemKeys, fluidKeys), input));
        var result = new ArrayList<>(candidates);
        result.sort(Comparator.comparingLong(recipe -> members.get(recipe).order()));
        return result;
    }
    private long version(Keys keys, boolean input) {
        var side = input ? inputs : outputs;
        long version = 0;
        for (var key : keys.items()) version += side.itemVersions.getOrDefault(key, 0L);
        for (int key : keys.fluids()) version += side.fluidVersions.getOrDefault(key, 0L);
        return version;
    }
    private static void collect(Set<Recipe> result, Set<Recipe> bucket) { if (bucket != null) result.addAll(bucket); }
}
