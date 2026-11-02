// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.compat;

import gregapi.code.ItemStackMap;
import gregapi.data.CS;
import gregapi.oredict.OreDictManager;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.ST;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Each recipe is indexed by one required input, favouring a small bucket over common circuits and molds. */
public final class RegistrationCollisionIndex {
    public interface Holder { RegistrationCollisionIndex gtqualityneo$collisionIndex(); }
    private final Map<Item, Map<Short, Set<Recipe>>> items = new IdentityHashMap<>();
    private final Map<Recipe, Key> anchors = new IdentityHashMap<>();
    private final Set<Recipe> unanchored = identitySet();
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();

    private static Set<Recipe> identitySet() { return Collections.newSetFromMap(new IdentityHashMap<>()); }

    private record Key(Item item, short meta) {
        static Key of(ItemStack stack) {
            Item item = stack.getItem();
            Item head = CS.Flattened.headItemOf(item);
            return new Key(head == null ? item : head, ST.meta_(stack));
        }
    }

    private Set<Recipe> bucket(Key key) {
        var byMeta = items.get(key.item());
        return byMeta == null ? null : byMeta.get(key.meta());
    }

    public synchronized void add(Recipe recipe) {
        if (anchors.containsKey(recipe) || unanchored.contains(recipe)) return;
        Key chosen = null;
        int smallest = Integer.MAX_VALUE;
        for (var input : recipe.mInputs) if (ST.valid(input)) {
            var key = Key.of(input);
            var bucket = bucket(key);
            int size = bucket == null ? 0 : bucket.size();
            if (size < smallest) { chosen = key; smallest = size; }
        }
        if (chosen == null) unanchored.add(recipe);
        else {
            items.computeIfAbsent(chosen.item(), unused -> new HashMap<>())
                .computeIfAbsent(chosen.meta(), unused -> identitySet()).add(recipe);
            anchors.put(recipe, chosen);
        }
    }

    public synchronized void remove(Recipe recipe) {
        var key = anchors.remove(recipe);
        if (key == null) { unanchored.remove(recipe); return; }
        var byMeta = items.get(key.item());
        var bucket = byMeta.get(key.meta());
        bucket.remove(recipe);
        if (bucket.isEmpty()) byMeta.remove(key.meta());
        if (byMeta.isEmpty()) items.remove(key.item());
    }

    public synchronized void rebuild(Collection<Recipe> recipes) {
        items.clear(); anchors.clear(); unanchored.clear();
        for (var recipe : recipes) add(recipe);
    }

    private void collect(ItemStack stack, Set<Recipe> result) {
        var key = Key.of(stack);
        var byMeta = items.get(key.item());
        if (byMeta == null) return;
        if (key.meta() == CS.W) byMeta.values().forEach(result::addAll);
        else {
            var exact = byMeta.get(key.meta());
            if (exact != null) result.addAll(exact);
            var wildcard = byMeta.get(CS.W);
            if (wildcard != null) result.addAll(wildcard);
        }
    }

    public synchronized Set<Recipe> candidates(ItemStack[] inputs) {
        var result = identitySet();
        result.addAll(unanchored);
        for (var input : inputs) if (ST.valid(input)) {
            collect(input, result);
            // Same direction as OreDictManager.equal_: a supplied input can match its unification target.
            var association = OreDictManager.INSTANCE.getAssociation_(input, true);
            if (association != null) {
                var target = association.mUnificationTarget;
                if (target == null) target = OreDictManager.INSTANCE.getStack(association.toString(), null, 1, false, false);
                if (ST.valid(target)) collect(target, result);
            }
        }
        return result;
    }

    public static Scope open(RecipeMap map, ItemStack[] inputs) {
        return new Scope(map, ((Holder) map).gtqualityneo$collisionIndex().candidates(inputs));
    }

    /** Only used inside registration's original findRecipeInternal call, never while a machine is running. */
    public static Object filter(ItemStackMap<?, ?> index, Object value) {
        var scope = ACTIVE.get();
        if (scope == null || scope.map.mRecipeItemMap != index || value == null) return value;
        @SuppressWarnings("unchecked") var original = (Collection<Recipe>) value;
        if (scope.allowed.size() >= original.size()) return value;
        Recipe single = null;
        int count = 0;
        for (var candidate : scope.allowed) if (original.contains(candidate)) {
            single = candidate;
            if (++count > 1) break;
        }
        if (count == 0) return List.of();
        if (count == 1) return List.of(single);
        // Keep original iteration order when several candidates remain, including RecipeCapture priority behavior.
        var result = new ArrayList<Recipe>();
        for (var candidate : original) if (scope.allowed.contains(candidate)) result.add(candidate);
        return result;
    }

    public static final class Scope implements AutoCloseable {
        private final RecipeMap map;
        private final Set<Recipe> allowed;
        private final Scope previous;
        private Scope(RecipeMap map, Set<Recipe> allowed) {
            this.map = map; this.allowed = allowed; previous = ACTIVE.get(); ACTIVE.set(this);
        }
        @Override public void close() { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }
}
