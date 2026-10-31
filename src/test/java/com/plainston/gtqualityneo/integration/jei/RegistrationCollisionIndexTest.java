package com.plainston.gtqualityneo.integration.jei;

import gregapi.data.CS;
import gregapi.data.OP;
import gregapi.data.MT;
import gregapi.code.ItemStackContainer;
import gregapi.code.ItemNBT;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.ST;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EphemeralTestServerProvider.class)
class RegistrationCollisionIndexTest {
    @Test void sharedSelectorsDoNotForceScanningEveryMaterial() {
        var index = new RegistrationCollisionIndex();
        var recipes = new ArrayList<Recipe>();
        for (int i = 1; i <= 3000; i++) {
            var recipe = recipe(stack(Items.IRON_NUGGET, i), new ItemStack(Items.REDSTONE));
            index.add(recipe);
            recipes.add(recipe);
        }
        assertTrue(index.candidates(new ItemStack[]{stack(Items.IRON_NUGGET, 31000), new ItemStack(Items.REDSTONE)}).isEmpty());
        var candidates = index.candidates(new ItemStack[]{stack(Items.IRON_NUGGET, 137), new ItemStack(Items.REDSTONE)});
        assertEquals(1, candidates.size());
        assertTrue(candidates.contains(recipes.get(136)));
    }

    @Test void candidatesAreASupersetOfOriginalMatchesIncludingSubsetsAndWildcards() {
        var catalyst = new ItemStack(Items.REDSTONE);
        ST.size_(0, catalyst);
        var fluidOnly = recipe();
        fluidOnly.mFluidInputs = new FluidStack[]{new FluidStack(Fluids.WATER, 100)};
        var recipes = List.of(recipe(new ItemStack(Items.IRON_INGOT)),
            recipe(new ItemStack(Items.IRON_INGOT), catalyst),
            recipe(stack(Items.IRON_INGOT, CS.W)), recipe(stack(Items.WHITE_WOOL, CS.W)),
            recipe(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT)), fluidOnly);
        var index = new RegistrationCollisionIndex();
        index.rebuild(recipes);
        var queries = List.of(new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
            new ItemStack[]{new ItemStack(Items.REDSTONE), new ItemStack(Items.IRON_INGOT)},
            new ItemStack[]{stack(Items.IRON_INGOT, 12)},
            new ItemStack[]{stack(Items.IRON_INGOT, CS.W)},
            new ItemStack[]{new ItemStack(Items.BLUE_WOOL)},
            new ItemStack[]{new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT)},
            new ItemStack[]{null, ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT)}, new ItemStack[]{});
        var fluids = new FluidStack[]{new FluidStack(Fluids.WATER, 100)};
        int matches = 0;
        for (var query : queries) {
            var candidates = index.candidates(query);
            for (var recipe : recipes) if (recipe.isRecipeInputEqual(false, true, fluids, query)) {
                matches++;
                assertTrue(candidates.contains(recipe), "An original match must never be pruned");
            }
        }
        assertTrue(matches > 10);
    }

    @Test void removingAndRebuildingUpdatesRegistrationKeys() {
        var index = new RegistrationCollisionIndex();
        var recipe = recipe(stack(Items.IRON_NUGGET, 17));
        index.add(recipe);
        ST.meta_(recipe.mInputs[0], 18);
        index.remove(recipe);
        assertTrue(index.candidates(new ItemStack[]{stack(Items.IRON_NUGGET, 17)}).isEmpty());
        index.rebuild(List.of(recipe));
        assertTrue(index.candidates(new ItemStack[]{stack(Items.IRON_NUGGET, 17)}).isEmpty());
        assertTrue(index.candidates(new ItemStack[]{stack(Items.IRON_NUGGET, 18)}).contains(recipe));
    }

    @Test void unificationTargetsRemainCandidatesInTheOriginalMatchingDirection() throws Exception {
        var supplied = stack(Items.BARRIER, 123);
        var data = new OreDictItemData(OP.ingot, MT.Fe);
        data.mUnificationTarget = new ItemStack(Items.IRON_INGOT);
        var field = OreDictManager.class.getDeclaredField("sItemStack2DataMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked") var associations = (Map<ItemStackContainer, OreDictItemData>) field.get(OreDictManager.INSTANCE);
        var key = new ItemStackContainer(supplied);
        var previous = associations.put(key, data);
        try {
            var recipe = recipe(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.REDSTONE));
            var query = new ItemStack[]{supplied, new ItemStack(Items.REDSTONE)};
            assertTrue(recipe.isRecipeInputEqual(false, true, new FluidStack[0], query));
            var index = new RegistrationCollisionIndex();
            index.add(recipe);
            assertTrue(index.candidates(query).contains(recipe));
        } finally {
            if (previous == null) associations.remove(key); else associations.put(key, previous);
        }
    }

    @Test void nbtSensitiveMatchingStillBelongsToGt6() {
        var required = new ItemStack(Items.IRON_INGOT);
        var requiredData = new CompoundTag();
        requiredData.putString("grade", "A");
        ItemNBT.set(required, requiredData);
        var recipe = recipe(required);
        var index = new RegistrationCollisionIndex();
        index.add(recipe);
        var supplied = new ItemStack(Items.IRON_INGOT);
        var otherData = new CompoundTag();
        otherData.putString("grade", "B");
        ItemNBT.set(supplied, otherData);
        var query = new ItemStack[]{supplied};
        assertTrue(index.candidates(query).contains(recipe));
        assertFalse(recipe.isRecipeInputEqual(false, true, new FluidStack[0], query));
    }

    @Test void filteringRetainsCandidateOrderAndDoesNotEscapeItsMapOrScope() {
        var map = new TestMap();
        try {
            var first = recipe(new ItemStack(Items.IRON_INGOT));
            var second = recipe(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.REDSTONE));
            var rejected = recipe(new ItemStack(Items.DIAMOND));
            map.index.rebuild(List.of(first, second, rejected));
            var bucket = new LinkedHashSet<>(List.of(second, rejected, first));
            assertSame(bucket, RegistrationCollisionIndex.filter(map.mRecipeItemMap, bucket));
            try (var scope = RegistrationCollisionIndex.open(map, new ItemStack[]{new ItemStack(Items.IRON_INGOT), new ItemStack(Items.REDSTONE)})) {
                assertEquals(List.of(second, first), RegistrationCollisionIndex.filter(map.mRecipeItemMap, bucket));
            }
            assertSame(bucket, RegistrationCollisionIndex.filter(map.mRecipeItemMap, bucket));
        } finally {
            RecipeMap.RECIPE_MAP_LIST.remove(map);
            RecipeMap.RECIPE_MAPS.remove(map.mNameInternal);
        }
    }

    private static ItemStack stack(Item item, long meta) { return ST.meta_(new ItemStack(item), meta); }
    private static Recipe recipe(ItemStack... inputs) {
        return new Recipe(false, false, inputs, new ItemStack[]{new ItemStack(Items.GOLD_INGOT)}, null,
            null, null, null, 20, 4, 0);
    }
    private static final class TestMap extends RecipeMap implements RegistrationCollisionIndex.Holder {
        private final RegistrationCollisionIndex index = new RegistrationCollisionIndex();
        TestMap() {
            super(new LinkedHashSet<>(), "gtqualityneo.test." + UUID.randomUUID(), "Test", null,
                0, 1, "", 2, 2, 0, 2, 2, 0, 0, 1, "", 1, "",
                false, false, false, false, true, false, true, true);
        }
        @Override public RegistrationCollisionIndex gtqualityneo$collisionIndex() { return index; }
    }
}
