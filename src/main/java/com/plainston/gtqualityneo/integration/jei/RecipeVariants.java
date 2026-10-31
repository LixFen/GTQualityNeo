package com.plainston.gtqualityneo.integration.jei;

import gregapi.recipes.Recipe;
import gregapi.util.ST;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/** Presentation-only alternatives. A single variable input avoids inventing combinations of correlated slots. */
public final class RecipeVariants {
    private RecipeVariants() {}

    public static List<Recipe> group(List<Recipe> recipes) {
        var groups = new LinkedHashMap<Object, List<Recipe>>();
        for (var recipe : recipes) {
            Object key = key(recipe);
            groups.computeIfAbsent(key == null ? recipe : key, unused -> new ArrayList<>()).add(recipe);
        }
        return groups.values().stream().map(members -> members.size() == 1
            ? members.getFirst() : (Recipe) new Grouped(members)).toList();
    }

    static Object key(Recipe recipe) {
        if (recipe.mFluidInputs.length != 1 || recipe.mFluidInputs[0] == null || recipe.mFluidInputs[0].isEmpty()
            || recipe.mSpecialItems != null && !(recipe.mSpecialItems instanceof ItemStack)) return null;
        int flags = (recipe.mFakeRecipe ? 1 : 0) | (recipe.mCanBeBuffered ? 2 : 0)
            | (recipe.mNeedsEmptyOutput ? 4 : 0) | (recipe.mNoNBTChecks ? 8 : 0);
        return new Signature(items(recipe.mInputs), items(recipe.mOutputs),
            Arrays.stream(recipe.mFluidOutputs).map(FluidKey::of).toList(),
            recipe.mSpecialItems instanceof ItemStack stack ? ItemKey.of(stack) : null,
            Arrays.stream(recipe.mChances).boxed().toList(), Arrays.stream(recipe.mMaxChances).boxed().toList(),
            recipe.mDuration, recipe.mEUt, recipe.mSpecialValue, flags, recipe.mFluidInputs[0].getAmount());
    }

    private static List<ItemKey> items(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(ItemKey::of).toList();
    }

    private record ItemKey(Item item, int count, DataComponentPatch components) {
        static ItemKey of(ItemStack stack) {
            return stack == null ? null : new ItemKey(stack.getItem(), ST.size(stack), stack.getComponentsPatch());
        }
    }

    private record FluidKey(Fluid fluid, int amount, DataComponentPatch components) {
        static FluidKey of(FluidStack stack) {
            return stack == null ? null : new FluidKey(stack.getFluid(), stack.getAmount(), stack.getComponentsPatch());
        }
    }

    private record Signature(List<ItemKey> inputs, List<ItemKey> outputs, List<FluidKey> fluidOutputs,
                             ItemKey special, List<Long> chances, List<Long> maximums,
                             long duration, long energy, long specialValue, int flags, int fluidAmount) {}

    public static List<FluidStack> inputOptions(Recipe recipe, int slot) {
        if (slot == 0 && recipe.mFluidInputs.length == 1 && recipe instanceof Grouped grouped) return grouped.options;
        return slot < recipe.mFluidInputs.length && recipe.mFluidInputs[slot] != null
            ? List.of(recipe.mFluidInputs[slot]) : List.of();
    }

    /** A private display copy, never inserted into a RecipeMap or used by a machine. */
    static final class Grouped extends Recipe {
        private List<FluidStack> options;

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
            setMembers(members);
        }

        private void setMembers(List<Recipe> recipes) {
            var fluids = new ArrayList<FluidStack>();
            for (var recipe : recipes) if (recipe.mFluidInputs.length == 1) {
                var fluid = recipe.mFluidInputs[0];
                if (fluid != null && fluids.stream().noneMatch(existing -> existing.getAmount() == fluid.getAmount()
                    && FluidStack.isSameFluidSameComponents(existing, fluid))) fluids.add(fluid.copy());
            }
            options = List.copyOf(fluids);
        }

        // Complete a streamed first card if on-demand registration extends its process group.
        void completeFrom(Recipe page) {
            options = inputOptions(page, 0).stream().map(FluidStack::copy).toList();
        }

    }
}
