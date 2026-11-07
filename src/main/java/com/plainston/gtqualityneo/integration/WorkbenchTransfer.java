package com.plainston.gtqualityneo.integration;

import gregapi.item.IItemGTContainerTool;
import gregapi.util.ST;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** Plans the complete move before changing any inventory. No REI classes are required on the server. */
public final class WorkbenchTransfer {
    private WorkbenchTransfer() {}

    public record Plan(List<Slot> slots, List<ItemStack> before, List<ItemStack> after, List<ItemStack> inputs) {
        public void apply() {
            for (int i = 0; i < slots.size(); i++)
                if (!ItemStack.matches(slots.get(i).getItem(), before.get(i))) return;
            for (int i = 0; i < slots.size(); i++)
                if (!ItemStack.matches(before.get(i), after.get(i))) slots.get(i).set(after.get(i).copy());
        }
    }

    public static boolean matches(ItemStack required, ItemStack actual, boolean tools) {
        if (actual.isEmpty()) return false;
        if (tools && required.getItem() instanceof IItemGTContainerTool) return ST.equalTools(required, actual, false);
        if (tools && ST.meta_(required) == gregapi.data.CS.W && ST.equal(required, actual, false)) {
            // GT6 ore entries can carry legacy wildcard metadata, including on vanilla building materials.
            // Normalize only that field; an explicit name or other component requirement stays significant.
            var normalized = actual.copy();
            ST.meta_(normalized, gregapi.data.CS.W);
            return required.getComponents().equals(normalized.getComponents());
        }
        return ItemStack.isSameItemSameComponents(required, actual);
    }

    public static Plan plan(List<Slot> grid, List<Slot> inventory, Player player,
                            List<List<ItemStack>> ingredients, boolean stacked, boolean tools) {
        if (grid.isEmpty() || ingredients.size() != grid.size()) return null;
        // An output slot cannot be disguised as an input in a client request: taking it requires onTake.
        for (var slot : grid)
            if (!slot.getItem().isEmpty() && !slot.allowModification(player)) return null;
        var slots = new ArrayList<>(grid);
        slots.addAll(inventory);
        if (new HashSet<>(slots).size() != slots.size()) return null;
        var before = slots.stream().map(slot -> slot.getItem().copy()).toList();
        int limit = 1;
        if (stacked) {
            limit = grid.stream().mapToInt(Slot::getMaxStackSize).max().orElse(1);
            // A reusable, unstackable tool occupies one cell while the workbench crafts repeatedly.
            if (ingredients.stream().flatMap(List::stream).anyMatch(stack -> stack.getMaxStackSize() == 1)) limit = 1;
        }
        for (int amount = limit; amount > 0; amount--) {
            var pool = before.stream().map(ItemStack::copy).toList();
            var desired = new ArrayList<>(Collections.nCopies(grid.size(), ItemStack.EMPTY));
            var after = assign(0, amount, slots, grid.size(), player, ingredients, tools, pool, desired);
            if (after != null) return new Plan(slots, before, after, List.copyOf(desired));
        }
        return null;
    }

    private static List<ItemStack> assign(int cell, int amount, List<Slot> slots, int gridSize, Player player,
            List<List<ItemStack>> ingredients, boolean tools, List<ItemStack> pool, List<ItemStack> desired) {
        if (cell == gridSize) return returnGrid(slots, gridSize, player, pool, desired);
        var target = slots.get(cell);
        if (!target.allowModification(player)) return null;
        if (ingredients.get(cell).isEmpty()) {
            desired.set(cell, ItemStack.EMPTY);
            return assign(cell + 1, amount, slots, gridSize, player, ingredients, tools, pool, desired);
        }
        var tried = new ArrayList<ItemStack>();
        for (var required : ingredients.get(cell)) {
            if (required.isEmpty()) continue;
            for (int source = 0; source < slots.size(); source++) {
                var actual = pool.get(source);
                if (!matches(required, actual, tools) || !slots.get(source).allowModification(player)
                    || source >= gridSize && !slots.get(source).mayPlace(actual)
                    || tried.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, actual))) continue;
                tried.add(actual.copy());
                long count = (long) required.getCount() * amount;
                if (count <= 0 || count > target.getMaxStackSize(actual) || !target.mayPlace(actual)) continue;
                var next = pool.stream().map(ItemStack::copy).toList();
                int remaining = (int) count;
                for (int i = 0; i < next.size() && remaining > 0; i++) {
                    var stack = next.get(i);
                    if (!ItemStack.isSameItemSameComponents(actual, stack) || !slots.get(i).allowModification(player)
                        || i >= gridSize && !slots.get(i).mayPlace(stack)) continue;
                    int taken = Math.min(remaining, stack.getCount());
                    stack.shrink(taken);
                    remaining -= taken;
                }
                if (remaining > 0) continue;
                desired.set(cell, actual.copyWithCount((int) count));
                var result = assign(cell + 1, amount, slots, gridSize, player, ingredients, tools, next, desired);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static List<ItemStack> returnGrid(List<Slot> slots, int gridSize, Player player,
                                             List<ItemStack> pool, List<ItemStack> desired) {
        var result = new ArrayList<ItemStack>();
        desired.forEach(stack -> result.add(stack.copy()));
        for (int i = gridSize; i < pool.size(); i++) result.add(pool.get(i).copy());
        for (int i = 0; i < gridSize; i++) {
            var remainder = pool.get(i).copy();
            if (!remainder.isEmpty() && !slots.get(i).allowModification(player)) return null;
            for (boolean empty : new boolean[]{false, true}) {
                for (int j = gridSize; j < slots.size() && !remainder.isEmpty(); j++) {
                    var stored = result.get(j);
                    var slot = slots.get(j);
                    if (stored.isEmpty() != empty || !slot.mayPlace(remainder) || !slot.allowModification(player)
                        || !stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, remainder)) continue;
                    int moved = Math.min(remainder.getCount(), slot.getMaxStackSize(remainder) - stored.getCount());
                    if (moved <= 0) continue;
                    result.set(j, remainder.copyWithCount(stored.getCount() + moved));
                    remainder.shrink(moved);
                }
            }
            if (!remainder.isEmpty()) return null;
        }
        return result;
    }

    public static void transfer(ServerPlayer player, int window, List<Integer> indices, int gridSize, List<ItemStack> inputs) {
        var menu = player.containerMenu;
        if (menu.containerId != window || !menu.stillValid(player) || gridSize <= 0
            || gridSize != inputs.size() || gridSize > indices.size() || indices.size() > menu.slots.size()
            || new HashSet<>(indices).size() != indices.size()
            || indices.stream().anyMatch(index -> index < 0 || index >= menu.slots.size())) return;
        var slots = indices.stream().map(menu::getSlot).toList();
        var ingredients = inputs.stream().map(stack -> stack.isEmpty() ? List.<ItemStack>of() : List.of(stack)).toList();
        var plan = plan(slots.subList(0, gridSize), slots.subList(gridSize, slots.size()), player, ingredients, false, false);
        if (plan != null) {
            plan.apply();
            menu.broadcastChanges();
        }
    }
}
