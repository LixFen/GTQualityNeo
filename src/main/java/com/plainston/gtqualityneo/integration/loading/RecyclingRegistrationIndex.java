package com.plainston.gtqualityneo.integration.loading;

import gregapi.oredict.OreDictItemData;
import gregapi.oredict.event.IOreDictListenerRecyclable.OreDictRecyclingContainer;
import gregapi.util.ST;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/** Mirrors GT6's recycling registrations; candidate snapshots retain their original insertion order. */
public final class RecyclingRegistrationIndex {
    private final Map<Item, Short2ObjectOpenHashMap<LinkedHashSet<OreDictRecyclingContainer>>> stacks = new IdentityHashMap<>();
    private final Map<OreDictItemData, LinkedHashSet<OreDictRecyclingContainer>> data = new IdentityHashMap<>();

    public RecyclingRegistrationIndex(Collection<OreDictRecyclingContainer> registrations) {
        registrations.forEach(this::add);
    }

    public void add(OreDictRecyclingContainer registration) {
        stacks.computeIfAbsent(registration.mStack.getItem(), unused -> new Short2ObjectOpenHashMap<>())
            .computeIfAbsent(ST.meta_(registration.mStack), unused -> new LinkedHashSet<>()).add(registration);
        data.computeIfAbsent(registration.mItemData, unused -> new LinkedHashSet<>()).add(registration);
    }

    public void remove(OreDictRecyclingContainer registration) {
        var metas = stacks.get(registration.mStack.getItem());
        if (metas != null) {
            short meta = ST.meta_(registration.mStack);
            var entries = metas.get(meta);
            if (entries != null && entries.remove(registration) && entries.isEmpty()) metas.remove(meta);
            if (metas.isEmpty()) stacks.remove(registration.mStack.getItem());
        }
        var entries = data.get(registration.mItemData);
        if (entries != null && entries.remove(registration) && entries.isEmpty()) data.remove(registration.mItemData);
    }

    public ArrayList<OreDictRecyclingContainer> forStack(ItemStack stack) {
        var metas = stacks.get(stack.getItem());
        var entries = metas == null ? null : metas.get(ST.meta_(stack));
        return entries == null ? new ArrayList<>() : new ArrayList<>(entries);
    }

    public ArrayList<OreDictRecyclingContainer> forData(OreDictItemData itemData) {
        var entries = data.get(itemData);
        return entries == null ? new ArrayList<>() : new ArrayList<>(entries);
    }
}
