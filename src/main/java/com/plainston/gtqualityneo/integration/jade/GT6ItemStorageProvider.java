package com.plainston.gtqualityneo.integration.jade;

import java.util.ArrayList;
import java.util.List;

import gregapi.block.multitileentity.example.MultiTileEntityChest;
import gregapi.tileentity.inventories.MultiTileEntityMassStorage;
import gregtech.tileentity.inventories.MultiTileEntityDrawerQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

/** GT6 storage slots have semantics that the generic capability view cannot express. */
public enum GT6ItemStorageProvider implements IServerExtensionProvider<ItemStack> {
    INSTANCE;
    static final Identifier UID = Identifier.fromNamespaceAndPath("gtqualityneo", "item_storage");

    @Override public Identifier getUid() { return UID; }
    @Override public int getDefaultPriority() { return -100; }

    @Override public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        Object target = accessor.getTarget();
        if (target instanceof MultiTileEntityMassStorage storage) return List.of(collect(storage, 1, 2));
        if (target instanceof MultiTileEntityDrawerQuad drawer) {
            List<ViewGroup<ItemStack>> groups = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) groups.add(collect(drawer, i * 36, (i + 1) * 36));
            return groups;
        }
        if (target instanceof MultiTileEntityChest chest) return List.of(collect(chest, 0, chest.getContainerSize()));
        return null;
    }

    static ViewGroup<ItemStack> collect(Container inventory, int from, int to) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = from; slot < Math.min(to, inventory.getContainerSize()); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty()) continue;
            ItemStack existing = null;
            for (ItemStack item : items) {
                if (ItemStack.isSameItemSameComponents(item, stack)
                    && (long) item.getCount() + stack.getCount() <= Integer.MAX_VALUE) {
                    existing = item;
                    break;
                }
            }
            if (existing == null) items.add(stack.copy());
            else existing.grow(stack.getCount());
        }
        return new ViewGroup<>(items);
    }
}
