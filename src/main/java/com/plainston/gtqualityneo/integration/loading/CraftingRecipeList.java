package com.plainston.gtqualityneo.integration.loading;

import gregapi.code.ArrayListNoNulls;
import gregapi.recipes.AdvancedCraftingShaped;
import gregapi.recipes.AdvancedCraftingShapeless;
import gregapi.recipes.ICraftingRecipeGT;
import gregapi.recipes.ShapedOreRecipe;
import gregapi.recipes.ShapelessOreRecipe;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.item.crafting.CraftingInput;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

/** The original mutable crafting list with an initialization-only index of necessary matching conditions. */
public final class CraftingRecipeList extends ArrayListNoNulls<ICraftingRecipeGT> {
    private static final long serialVersionUID = 1L;
    private transient Index index;
    private transient boolean viewsExposed;

    public CraftingRecipeList(int capacity) { super(capacity); }

    @Override public boolean add(ICraftingRecipeGT recipe) {
        int before = modCount, position = size();
        boolean added = super.add(recipe);
        if (added && index != null && index.version == before) {
            index.add(position, recipe);
            index.version = modCount;
        }
        return added;
    }

    @Override public ICraftingRecipeGT set(int position, ICraftingRecipeGT recipe) {
        releaseIndex();
        return super.set(position, recipe);
    }
    @Override public void replaceAll(UnaryOperator<ICraftingRecipeGT> operator) {
        releaseIndex(); // ArrayList may have changed some elements before an operator throws.
        super.replaceAll(operator);
    }
    @Override public void sort(Comparator<? super ICraftingRecipeGT> comparator) {
        releaseIndex();
        super.sort(comparator);
    }
    @Override public List<ICraftingRecipeGT> subList(int from, int to) {
        var view = super.subList(from, to);
        // ArrayList.SubList.set bypasses the root's set/modCount. Preserve the native view and use full scans.
        viewsExposed = true;
        releaseIndex();
        return view;
    }
    @Override public Object clone() {
        var copy = (CraftingRecipeList) super.clone();
        copy.index = null;
        copy.viewsExposed = false;
        return copy;
    }
    public void releaseIndex() { index = null; }

    public int nextCandidate(int from, CraftingInput grid) {
        if (viewsExposed) return from;
        if (index == null || index.version != modCount) {
            index = new Index();
            for (int i = 0; i < size(); i++) index.add(i, get(i));
            index.version = modCount;
        }
        int next = next(index.shaped.get(shape(grid.width(), grid.height())), from, size());
        int occupied = -1;
        for (int p = lowerBound(index.fallback, from); p < index.fallback.size(); p++) {
            int position = index.fallback.getInt(p);
            if (position >= next) break;
            var recipe = get(position);
            if (recipe != null && (recipe.getClass() == ShapelessOreRecipe.class || recipe.getClass() == AdvancedCraftingShapeless.class)) {
                if (occupied < 0) {
                    occupied = 0;
                    // A custom matcher may mutate stacks after CraftingInput cached ingredientCount.
                    for (int i = 0; i < grid.size(); i++) if (!grid.getItem(i).isEmpty()) occupied++;
                }
                if (((ShapelessOreRecipe) recipe).getInput().size() != occupied) continue;
            }
            return position;
        }
        return next;
    }

    private static long shape(int width, int height) { return ((long) width << 32) | (height & 0xffffffffL); }
    private static int next(IntArrayList positions, int from, int end) {
        if (positions == null) return end;
        int p = lowerBound(positions, from);
        return p == positions.size() ? end : positions.getInt(p);
    }
    private static int lowerBound(IntArrayList positions, int from) {
        int low = 0, high = positions.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (positions.getInt(mid) < from) low = mid + 1; else high = mid;
        }
        return low;
    }
    private static final class Index {
        private final Long2ObjectOpenHashMap<IntArrayList> shaped = new Long2ObjectOpenHashMap<>();
        private final IntArrayList fallback = new IntArrayList();
        private int version;
        private void add(int position, ICraftingRecipeGT recipe) {
            var type = recipe == null ? null : recipe.getClass();
            if (type == AdvancedCraftingShaped.class && !((AdvancedCraftingShaped) recipe).mRemovableByGT) return;
            if (type == AdvancedCraftingShapeless.class && !((AdvancedCraftingShapeless) recipe).mRemovableByGT) return;
            if (type == ShapedOreRecipe.class || type == AdvancedCraftingShaped.class) {
                var shapedRecipe = (ShapedOreRecipe) recipe;
                shaped.computeIfAbsent(shape(shapedRecipe.getWidth(), shapedRecipe.getHeight()), unused -> new IntArrayList()).add(position);
            } else {
                fallback.add(position);
            }
        }
    }
}
