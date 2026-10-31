package com.plainston.gtqualityneo.integration.jei;

import gregapi.recipes.IRecipeMapHandler.RecipeMapHandler;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.ST;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EphemeralTestServerProvider.class)
class MachineRecipeLookupTest {
    private final List<CountingMap> maps = new ArrayList<>();

    @AfterEach void cleanup() {
        for (var map : maps) {
            RecipeMap.RECIPE_MAP_LIST.remove(map);
            RecipeMap.RECIPE_MAPS.remove(map.mNameInternal);
        }
    }

    private CountingMap map() {
        var map = new CountingMap();
        maps.add(map);
        return map;
    }

    static Recipe recipe(FluidStack fluid) {
        return new Recipe(false, false, new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
            new ItemStack[]{new ItemStack(Items.GOLD_INGOT)}, null, new long[]{2500},
            new FluidStack[]{fluid}, null, 200, 32, 0);
    }

    @Test void equivalentFluidsShareACardWithoutChangingMachineRecipes(MinecraftServer server) {
        var water = recipe(new FluidStack(Fluids.WATER, 100));
        var lava = recipe(new FluidStack(Fluids.LAVA, 100));
        var grouped = RecipeVariants.group(List.of(water, lava));
        assertEquals(1, grouped.size());
        var page = grouped.getFirst();
        assertNotSame(water, page);
        assertEquals(2, RecipeVariants.inputOptions(page, 0).size());
        assertEquals(2500, page.getOutputChance(0));
        assertEquals(200, page.mDuration);
        assertEquals(32, page.mEUt);
        page.mInputs[0].setCount(3);
        assertEquals(1, water.mInputs[0].getCount());
        assertEquals(1, lava.mInputs[0].getCount());
    }

    @Test void differentCostsOutputsProbabilitiesComponentsAndCorrelatedFluidsStaySeparate() {
        var base = recipe(new FluidStack(Fluids.WATER, 100));
        for (int difference = 0; difference < 10; difference++) {
            var other = recipe(new FluidStack(Fluids.LAVA, 100));
            switch (difference) {
                case 0 -> other.mDuration++;
                case 1 -> other.mEUt++;
                case 2 -> other.mFluidInputs[0].setAmount(200);
                case 3 -> other.mChances[0]++;
                case 4 -> other.mMaxChances[0]++;
                case 5 -> other.mOutputs[0] = new ItemStack(Items.DIAMOND);
                case 6 -> other.mInputs[0].set(DataComponents.CUSTOM_NAME, Component.literal("special material"));
                case 7 -> other.mInputs[0].setCount(2);
                case 8 -> other.mSpecialValue++;
                case 9 -> ST.meta_(other.mInputs[0], 42);
            }
            assertEquals(2, RecipeVariants.group(List.of(base, other)).size(), "difference " + difference);
        }
        var second = base.copy();
        base.mFluidInputs = new FluidStack[]{new FluidStack(Fluids.WATER, 100), new FluidStack(Fluids.LAVA, 100)};
        second.mFluidInputs = new FluidStack[]{new FluidStack(Fluids.LAVA, 100), new FluidStack(Fluids.WATER, 100)};
        assertEquals(2, RecipeVariants.group(List.of(base, second)).size());
    }

    @Test void catalystsRemainUnconsumedOnTheGroupedDisplayCopy() {
        var water = recipe(new FluidStack(Fluids.WATER, 100));
        var lava = recipe(new FluidStack(Fluids.LAVA, 100));
        ST.size_(0, water.mInputs[0]);
        ST.size_(0, lava.mInputs[0]);
        var page = RecipeVariants.group(List.of(water, lava)).getFirst();
        assertEquals(0, ST.size(page.mInputs[0]));
        assertEquals(0, ST.size(water.mInputs[0]));
        assertEquals(2, RecipeVariants.inputOptions(page, 0).size());
    }

    @Test void startupProbeIsCheapAndFirstFullQueryVisitsEveryHandlerOnce() {
        var map = map();
        var first = recipe(new FluidStack(Fluids.WATER, 100));
        map.add(first, false, false);
        var calls = new AtomicInteger();
        for (int i = 0; i < 8; i++) {
            final int amount = i + 101;
            map.add(new RecipeMapHandler() {
                private boolean done;
                @Override public boolean addAllRecipes(RecipeMap target) {
                    calls.incrementAndGet();
                    target.add(recipe(new FluidStack(Fluids.WATER, amount)), false, false);
                    done = true;
                    return true;
                }
                @Override public boolean isDone() { return done; }
            });
        }
        var lookup = new MachineRecipeLookup(map, () -> true);
        assertTrue(lookup.lazyAll().stream().findFirst().isPresent());
        assertEquals(0, calls.get());
        assertEquals(9, lookup.lazyAll().stream().toList().size());
        assertEquals(8, calls.get());
        assertTrue(map.mRecipeMapHandlers.isEmpty());
        assertSame(lookup.all(), lookup.all());
        assertEquals(8, calls.get());
    }

    @Test void lazyFirstCardReceivesAllAlternativesWithoutDuplicatingIt() {
        var map = map();
        map.add(recipe(new FluidStack(Fluids.WATER, 100)), false, false);
        map.add(recipe(new FluidStack(Fluids.LAVA, 100)), false, false);
        var lookup = new MachineRecipeLookup(map, () -> true);
        var streamed = lookup.lazyAll().stream().toList();
        assertEquals(1, streamed.size());
        assertEquals(2, RecipeVariants.inputOptions(streamed.getFirst(), 0).size());
    }

    @Test void registrationGroupsExistBeforeAnyJeiQueryAndQueriesReuseThem() {
        var map = map();
        var water = recipe(new FluidStack(Fluids.WATER, 100));
        var lava = recipe(new FluidStack(Fluids.LAVA, 100));
        map.add(water, false, false);
        map.add(lava, false, false);
        var registry = map.gtqualityneo$recipeVariants();
        assertEquals(1, registry.groupCount(), "Grouping takes place during registration");
        var registeredPage = registry.pageFor(water);
        assertSame(registeredPage, registry.pageFor(lava));
        var lookup = new MachineRecipeLookup(map, () -> true);
        assertSame(registeredPage, lookup.all().getFirst());
        map.queryResults = List.of(water);
        assertSame(registeredPage, lookup.find(new ItemStack(Items.IRON_INGOT), true).getFirst());
        assertEquals(2, RecipeVariants.inputOptions(registeredPage, 0).size(), "A focused query keeps the process's alternatives");
        assertEquals(2, map.mRecipeList.size());
    }

    @Test void customAllRecipesKeepsVirtualRecipesOutsideTheRegistrationGroups() {
        var virtual = recipe(new FluidStack(Fluids.WATER, 500));
        var map = new CountingMap() {
            @Override public List<Recipe> getNEIAllRecipes() {
                var recipes = new ArrayList<>(super.getNEIAllRecipes());
                recipes.add(virtual);
                return recipes;
            }
        };
        maps.add(map);
        map.add(recipe(new FluidStack(Fluids.WATER, 100)), false, false);
        var lookup = new MachineRecipeLookup(map, () -> true);
        assertEquals(2, lookup.all().size());
        assertTrue(lookup.all().contains(virtual));
        assertEquals(1, map.gtqualityneo$recipeVariants().groupCount());
        assertEquals(1, map.mRecipeList.size());
    }

    @Test void focusCacheIsBoundedAndInvalidatedOnMutationAndOptionChanges() {
        var map = map();
        map.add(recipe(new FluidStack(Fluids.WATER, 100)), false, false);
        map.add(recipe(new FluidStack(Fluids.LAVA, 100)), false, false);
        var merge = new AtomicBoolean(true);
        var lookup = new MachineRecipeLookup(map, merge::get);
        var stack = new ItemStack(Items.IRON_INGOT);
        assertSame(lookup.find(stack, true), lookup.find(stack.copy(), true));
        assertEquals(1, map.queries);
        lookup.find(stack, false);
        assertEquals(2, map.queries, "Input/output queries have separate keys");
        map.revision++;
        lookup.find(stack, true);
        assertEquals(3, map.queries, "A rebuild invalidates even when recipe count stays the same");
        merge.set(false);
        assertEquals(2, lookup.find(stack, true).size());
        for (int i = 0; i < 65; i++) {
            var named = stack.copy();
            named.set(DataComponents.CUSTOM_NAME, Component.literal("focus " + i));
            lookup.find(named, true);
        }
        int before = map.queries;
        lookup.find(stack, true);
        assertEquals(before + 1, map.queries, "Old focus entries are evicted");
    }

    private static class CountingMap extends RecipeMap implements RecipeMapRevision {
        private int queries;
        private long revision;
        private final RegisteredRecipeVariants variants = new RegisteredRecipeVariants();
        private List<Recipe> queryResults;
        CountingMap() {
            super(new LinkedHashSet<>(), "gtqualityneo.test." + UUID.randomUUID(), "Test", null,
                0, 1, "", 2, 2, 1, 2, 2, 0, 1, 1, "", 1, "",
                false, false, false, false, true, false, true, true);
        }
        @Override public long gtqualityneo$recipeRevision() { return revision; }
        @Override public RegisteredRecipeVariants gtqualityneo$recipeVariants() { return variants; }
        @Override public synchronized Recipe add(Recipe recipe, boolean logErrors, boolean addConfig) {
            var result = super.add(recipe, logErrors, addConfig);
            if (result != null && mRecipeList.contains(result)) {
                variants.add(result);
                revision++;
            }
            return result;
        }
        @Override public List<Recipe> getNEIUsagesFor(ItemStack stack) { queries++; return queryResults == null ? List.copyOf(mRecipeList) : queryResults; }
        @Override public List<Recipe> getNEIRecipesFor(ItemStack stack) { queries++; return queryResults == null ? List.copyOf(mRecipeList) : queryResults; }
    }
}
