package com.plainston.gtqualityneo.integration.loading;

import gregapi.data.CS;
import gregapi.util.ST;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Index only the copied blacklist of an immutable furnace snapshot, never the mutable live list. */
public final class FurnaceSnapshotKeys extends AbstractList<ItemStack> {
    private final List<ItemStack> keys;
    private final Map<Item, List<ItemStack>> families = new IdentityHashMap<>();

    public FurnaceSnapshotKeys(List<ItemStack> keys) {
        this.keys = keys;
        for (var key : keys) if (key != null) {
            families.computeIfAbsent(family(key), unused -> new ArrayList<>()).add(key);
        }
    }

    private static Item family(ItemStack stack) {
        var item = stack.getItem();
        var head = CS.Flattened.headItemOf(item);
        return head == null ? item : head;
    }

    public boolean matches(ItemStack input) {
        if (input == null) return false;
        var candidates = families.get(family(input));
        if (candidates != null) for (var key : candidates) if (ST.equal(input, key, true)) return true;
        return false;
    }

    @Override public ItemStack get(int index) { return keys.get(index); }
    @Override public int size() { return keys.size(); }
}
