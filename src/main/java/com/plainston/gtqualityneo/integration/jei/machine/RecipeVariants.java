// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.machine;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.recipes.Recipe;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Presentation-only alternatives. A card holds real member recipes forming a complete product of its slots' options,
 * so each slot lists its options once and any combination shown on the card is a recipe GT6 registered.
 */
public final class RecipeVariants {
    private RecipeVariants() {}
    public static long ingredientVersion(Recipe recipe) { return recipe instanceof Grouped grouped ? grouped.version : 0; }

    /** Which slot kinds may vary on one card. Fluids: the single fluid input. Ore dictionary: GT6 material items. */
    public record Mode(boolean fluids, boolean oreDict) {
        public static final Mode FLUIDS = new Mode(true, false), ALL = new Mode(true, true);
        public boolean any() { return fluids || oreDict; }
    }

    public static List<Recipe> group(List<Recipe> recipes, Mode mode) {
        var buckets = new LinkedHashMap<Object, List<Recipe>>();
        for (var recipe : recipes) {
            Object key = key(recipe), part = key == null ? null : partition(recipe, mode);
            buckets.computeIfAbsent(part == null ? recipe : Arrays.asList(key, part), unused -> new ArrayList<>()).add(recipe);
        }
        return buckets.values().stream().flatMap(bucket -> pages(bucket).stream()).toList();
    }

    /**
     * Cards for one bucket. Each card's members are exactly every combination of its slots' options, so every slot can
     * list its options once and cycle on its own without suggesting a combination that GT6 never registered.
     */
    static List<Recipe> pages(List<Recipe> members) {
        var cards = new ArrayList<Recipe>();
        for (var block : blocks(members, slots(members.getFirst())))
            cards.add(block.size() == 1 ? block.getFirst() : new Grouped(block));
        return cards;
    }

    private static int slots(Recipe recipe) { return recipe.mInputs.length + 1; }

    /** The value a member has in a slot: the item inputs, then the single fluid input. */
    private static Object value(Recipe recipe, int slot) {
        if (slot < recipe.mInputs.length) return ItemKey.of(recipe.mInputs[slot]);
        return recipe.mFluidInputs.length == 0 ? null : FluidKey.of(recipe.mFluidInputs[0]);
    }

    /**
     * Splits members into complete products. Members agreeing on the remaining slots form one row; rows offering the same
     * options in the last slot are merged and the rows themselves split again on the slots before it.
     */
    private static List<List<Recipe>> blocks(List<Recipe> members, int slots) {
        if (slots <= 1 || members.size() == 1) return List.of(members);
        int last = slots - 1;
        var rows = new LinkedHashMap<List<Object>, List<Recipe>>();
        for (var member : members) {
            var rest = new ArrayList<Object>(last);
            for (int i = 0; i < last; i++) rest.add(value(member, i));
            rows.computeIfAbsent(rest, unused -> new ArrayList<>()).add(member);
        }
        // Group the rows by the set of options they offer in the last slot.
        var byOptions = new LinkedHashMap<java.util.Set<Object>, List<List<Recipe>>>();
        for (var row : rows.values()) {
            var options = new java.util.LinkedHashSet<Object>();
            for (var member : row) options.add(value(member, last));
            byOptions.computeIfAbsent(options, unused -> new ArrayList<>()).add(row);
        }
        var result = new ArrayList<List<Recipe>>();
        for (var sameOptions : byOptions.values()) {
            var heads = sameOptions.stream().map(List::getFirst).toList();
            var rowOf = new IdentityHashMap<Recipe, List<Recipe>>();
            for (var row : sameOptions) rowOf.put(row.getFirst(), row);
            for (var block : blocks(heads, last)) {
                var merged = new ArrayList<Recipe>();
                for (var head : block) merged.addAll(rowOf.get(head));
                result.add(merged);
            }
        }
        return result;
    }

    /**
     * Registration key with every slot that may vary under some mode left open; {@link #partition} closes the slots the
     * current mode keeps fixed. Two recipes can only share a card if they share this key.
     */
    static Object key(Recipe recipe) {
        if (recipe.mSpecialItems != null && !(recipe.mSpecialItems instanceof ItemStack)) return null;
        boolean openFluid = singleFluid(recipe);
        boolean openItem = Arrays.stream(recipe.mInputs).anyMatch(stack -> ore(stack) != null);
        if (!openFluid && !openItem) return null;
        int flags = (recipe.mFakeRecipe ? 1 : 0) | (recipe.mCanBeBuffered ? 2 : 0)
            | (recipe.mNeedsEmptyOutput ? 4 : 0) | (recipe.mNoNBTChecks ? 8 : 0);
        var inputs = Arrays.stream(recipe.mInputs).<Object>map(stack -> {
            var ore = ore(stack);
            return ore != null ? ore.slot() : ItemKey.of(stack);
        }).toList();
        var fluids = openFluid ? List.of((Object) recipe.mFluidInputs[0].getAmount())
            : Arrays.stream(recipe.mFluidInputs).map(stack -> (Object) FluidKey.of(stack)).toList();
        return new Signature(inputs, items(recipe.mOutputs), fluids,
            Arrays.stream(recipe.mFluidOutputs).map(FluidKey::of).toList(),
            recipe.mSpecialItems instanceof ItemStack stack ? ItemKey.of(stack) : null,
            Arrays.stream(recipe.mChances).boxed().toList(), Arrays.stream(recipe.mMaxChances).boxed().toList(),
            recipe.mDuration, recipe.mEUt, recipe.mSpecialValue, flags);
    }

    /** What must still match within one key under this mode; null when the mode lets nothing vary. */
    static Object partition(Recipe recipe, Mode mode) {
        if (!mode.any()) return null;
        var parts = new ArrayList<Object>();
        for (var stack : recipe.mInputs) {
            var ore = ore(stack);
            // A material varies only inside its family of GT6 "Any" materials, e.g. Steel, Knightmetal, Meteoric Steel.
            parts.add(ore == null ? null : mode.oreDict ? family(ore.material) : ore.material);
        }
        parts.add(singleFluid(recipe) && !mode.fluids ? FluidKey.of(recipe.mFluidInputs[0]) : null);
        return parts;
    }

    private static boolean singleFluid(Recipe recipe) {
        return recipe.mFluidInputs.length == 1 && recipe.mFluidInputs[0] != null && !recipe.mFluidInputs[0].isEmpty();
    }

    /** A plain GT6 prefix item (the stack GT6 itself makes for that prefix and material), else null. */
    private static Ore ore(ItemStack stack) {
        if (ST.invalid(stack)) return null;
        var data = OM.anydata_(stack);
        if (data == null || !data.validData() || data.mMaterial.mMaterial == null) return null;
        var canonical = data.mPrefix.mat(data.mMaterial.mMaterial, 1);
        if (ST.invalid(canonical) || canonical.getItem() != stack.getItem()
            || !canonical.getComponentsPatch().equals(stack.getComponentsPatch())) return null;
        return new Ore(new OreSlot(data.mPrefix, ST.size(stack)), data.mMaterial.mMaterial);
    }

    private record Ore(OreSlot slot, OreDictMaterial material) {}
    private record OreSlot(OreDictPrefix prefix, long count) {}

    private static final Map<OreDictMaterial, Object> FAMILIES = new IdentityHashMap<>();

    /** Materials joined through GT6 re-registration ("Any" materials) share one family token. */
    static synchronized Object family(OreDictMaterial material) {
        var known = FAMILIES.get(material);
        if (known != null) return known;
        var token = new Object();
        var queue = new ArrayDeque<OreDictMaterial>();
        queue.add(material);
        FAMILIES.put(material, token);
        while (!queue.isEmpty()) {
            var next = queue.poll();
            for (var linked : List.of(next.mReRegistrations, next.mToThis))
                for (var other : linked) if (FAMILIES.putIfAbsent(other, token) == null) queue.add(other);
        }
        return token;
    }

    private static List<ItemKey> items(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(ItemKey::of).toList();
    }

    private record ItemKey(Item item, long count, DataComponentPatch components) {
        static ItemKey of(ItemStack stack) {
            return stack == null ? null : new ItemKey(stack.getItem(), ST.size(stack), stack.getComponentsPatch());
        }
    }

    private record FluidKey(Fluid fluid, int amount, DataComponentPatch components) {
        static FluidKey of(FluidStack stack) {
            return stack == null ? null : new FluidKey(stack.getFluid(), stack.getAmount(), stack.getComponentsPatch());
        }
    }

    private record Signature(List<Object> inputs, List<ItemKey> outputs, List<Object> fluidInputs, List<FluidKey> fluidOutputs,
                             ItemKey special, List<Long> chances, List<Long> maximums,
                             long duration, long energy, long specialValue, int flags) {}

    /** The distinct item input options of a slot, in member order. */
    public static List<ItemStack> itemOptions(Recipe recipe, int slot) {
        if (recipe instanceof Grouped grouped && slot < recipe.mInputs.length) {
            var seen = new java.util.HashSet<Object>();
            return grouped.members.stream().filter(member -> seen.add(ItemKey.of(member.mInputs[slot])))
                .map(member -> member.getRepresentativeInput(slot)).filter(Objects::nonNull).toList();
        }
        var stack = recipe.getRepresentativeInput(slot);
        return stack == null ? List.of() : List.of(stack);
    }

    /** The distinct fluid input options of a slot, in member order. */
    public static List<FluidStack> inputOptions(Recipe recipe, int slot) {
        if (recipe instanceof Grouped grouped && slot == 0 && singleFluid(recipe)) {
            var seen = new java.util.HashSet<Object>();
            return grouped.members.stream().filter(member -> seen.add(FluidKey.of(member.mFluidInputs[0])))
                .map(member -> member.mFluidInputs[0].copy()).toList();
        }
        return slot < recipe.mFluidInputs.length && recipe.mFluidInputs[slot] != null
            ? List.of(recipe.mFluidInputs[slot]) : List.of();
    }

    /** A private display copy, never inserted into a RecipeMap or used by a machine. */
    static final class Grouped extends Recipe {
        private List<Recipe> members;
        private long version;

        Grouped(List<Recipe> members) {
            super(false, false, null, null, null, null, null, null, 1, 0, 0);
            var source = members.getFirst().copy();
            mInputs = source.mInputs;
            mOutputs = source.mOutputs;
            mFluidInputs = source.mFluidInputs;
            mFluidOutputs = source.mFluidOutputs;
            mSpecialItems = source.mSpecialItems;
            mChances = source.mChances;
            mMaxChances = source.mMaxChances;
            mDuration = source.mDuration;
            mEUt = source.mEUt;
            mSpecialValue = source.mSpecialValue;
            mEnabled = source.mEnabled;
            mHidden = source.mHidden;
            mFakeRecipe = source.mFakeRecipe;
            mCanBeBuffered = source.mCanBeBuffered;
            mNeedsEmptyOutput = source.mNeedsEmptyOutput;
            mNoNBTChecks = source.mNoNBTChecks;
            this.members = List.copyOf(members);
        }

        List<Recipe> members() { return members; }

        // Complete a streamed first card if on-demand registration extends its process group.
        void completeFrom(Recipe page) {
            var next = page instanceof Grouped grouped ? grouped.members : List.of(page);
            if (!members.equals(next)) { members = next; version++; }
        }
    }
}
