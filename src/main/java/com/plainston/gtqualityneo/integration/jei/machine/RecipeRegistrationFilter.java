package com.plainston.gtqualityneo.integration.jei.machine;

import gregapi.data.CS;
import gregapi.oredict.OreDictManager;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.ST;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.IdentityHashMap;
import java.util.Map;

/** A conservative input precheck for a bulk generation batch. GT6 still decides every possible match. */
public final class RecipeRegistrationFilter implements AutoCloseable {
    private static final ThreadLocal<RecipeRegistrationFilter> ACTIVE = new ThreadLocal<>();
    private final RecipeRegistrationFilter parent;
    private final RecipeMap map;
    private RecipeMap queryMap;
    private Map<Item, ShortSet> inputs;

    private RecipeRegistrationFilter(RecipeMap map) {
        this.map = map;
        parent = ACTIVE.get();
        ACTIVE.set(this);
    }

    public static RecipeRegistrationFilter begin(RecipeMap map) { return new RecipeRegistrationFilter(map); }

    /** A loader batch registers into multiple maps; each nested query still owns its input precheck. */
    public static RecipeRegistrationFilter beginAll() { return new RecipeRegistrationFilter(null); }

    public static Query query(RecipeMap map, boolean enabled, ItemStack[] supplied) {
        var filter = ACTIVE.get();
        if (filter == null || (filter.map != null && filter.map != map)) return null;
        Map<Item, ShortSet> keys = null;
        if (enabled) {
            keys = new IdentityHashMap<>();
            if (supplied != null) for (var stack : supplied) if (ST.valid(stack)) {
                add(keys, stack);
                var data = OreDictManager.INSTANCE.getAssociation_(stack, true);
                if (data != null) {
                    // equal_ compares the supplied stack and its target against a required stack. This getter also
                    // covers an unresolved target; its first-ore fallback only admits extra candidates.
                    var target = data.mUnificationTarget;
                    if (ST.invalid(target)) target = OreDictManager.INSTANCE.getStack(data.toString(), null, 1, false, false);
                    add(keys, target);
                }
            }
        }
        return new Query(filter, map, keys);
    }

    private static Item family(Item item) {
        var head = CS.Flattened.headItemOf(item);
        return head == null ? item : head;
    }

    private static void add(Map<Item, ShortSet> keys, ItemStack stack) {
        if (ST.valid(stack)) keys.computeIfAbsent(family(stack.getItem()), unused -> new ShortOpenHashSet()).add(ST.meta_(stack));
    }

    public static boolean mayMatch(RecipeMap map, Recipe recipe) {
        var filter = ACTIVE.get();
        if (filter == null || filter.queryMap != map || filter.inputs == null || recipe.getClass() != Recipe.class) return true;
        // Even a size-zero catalyst is required by checkStacksEqual. One impossible required input is enough
        // to reject a candidate, but fluids, amounts, NBT, input order and priority stay with the original code.
        for (var required : recipe.mInputs) if (ST.valid(required)) {
            var metas = filter.inputs.get(family(required.getItem()));
            if (metas == null) return false;
            short meta = ST.meta_(required);
            if (meta != CS.W && !metas.contains(CS.W) && !metas.contains(meta)) return false;
            // Keep the existing first-input shortcut for map-specific JEI batches.
            if (filter.map != null) return true;
        }
        return true;
    }

    public static final class Query implements AutoCloseable {
        private final RecipeRegistrationFilter filter;
        private final Map<Item, ShortSet> previous;
        private final RecipeMap previousMap;
        private Query(RecipeRegistrationFilter filter, RecipeMap map, Map<Item, ShortSet> inputs) {
            this.filter = filter;
            previous = filter.inputs;
            previousMap = filter.queryMap;
            filter.queryMap = map;
            filter.inputs = inputs;
        }
        @Override public void close() { filter.inputs = previous; filter.queryMap = previousMap; }
    }

    @Override public void close() {
        if (parent == null) ACTIVE.remove(); else ACTIVE.set(parent);
    }
}
