package com.plainston.gtqualityneo.integration.worldgen;

import static org.junit.jupiter.api.Assertions.*;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Set;
import java.util.List;

@ExtendWith(EphemeralTestServerProvider.class)
class WorldgenCatalogTest {
    @org.junit.jupiter.api.BeforeAll static void initialize(net.minecraft.server.MinecraftServer server) throws Exception {
        // Loading the level runs GT6's deferred material, recipe and worldgen initialization.
        if (server.overworld() == null) {
            server.getWorldData().overworldData().setInitialized(true);
            var createLevels = net.minecraft.server.MinecraftServer.class.getDeclaredMethod("createLevels");
            createLevels.setAccessible(true);
            server.submit(() -> {
                try { createLevels.invoke(server); }
                catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
            }).get();
        }
    }
    @Test void modernHeightWindowUsesGT6RemapAndPhysicalDeepLayerLimit() {
        var world = (net.minecraft.world.level.LevelAccessor) java.lang.reflect.Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{net.minecraft.world.level.LevelAccessor.class}, (proxy, method, arguments) ->
                switch (method.getName()) {
                    case "getMinY" -> -64;
                    case "getMaxY" -> 319;
                    case "getSeaLevel" -> 63;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var window = new WorldgenCatalog.HeightWindow(0, 62, true, 10, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(world.getMinY() + "–" + (world.getSeaLevel() - 1), window.height(world));
        float scaled = 10 * gregapi.util.WD.yStretch(world, 0, 62);
        assertEquals((int) scaled + "–" + (int) Math.ceil(scaled), window.attempts(world));
        var shallow = new WorldgenCatalog.HeightWindow(0, 80, false, 0, false, 24, Integer.MAX_VALUE);
        assertTrue(shallow.height(world).startsWith("24–"));
        var deep = new WorldgenCatalog.HeightWindow(0, 80, false, 0, false, Integer.MIN_VALUE, 23);
        assertEquals(world.getMinY() + "–23", deep.height(world));
        var remappedDeep = new WorldgenCatalog.HeightWindow(40, 80, false, 0, false, Integer.MIN_VALUE, 23);
        assertEquals("18–23", remappedDeep.height(world));
    }
    @Test void fluidDisplayWithZeroAmountMatchesFluidDeposit() {
        var page = new WorldgenCatalog.Page("spring", "test");
        page.fluid = net.minecraft.world.level.material.Fluids.WATER;
        assertTrue(page.matches(gregapi.data.FL.display(page.fluid), true));
        assertFalse(page.matches(gregapi.data.FL.display(net.minecraft.world.level.material.Fluids.LAVA), true));
    }
    @Test void oreMaterialLookupWorksAcrossDifferentPhysicalHosts() {
        var page = new WorldgenCatalog.Page("layer", "test");
        page.resource("ore", MT.Fe, new ItemStack(Items.IRON_ORE));
        assertTrue(page.matches(OP.oreSmall.mat(MT.Fe, 1), false));
        assertTrue(page.matches(OP.oreSmall.mat(MT.Fe, 1), true));
        assertFalse(page.matches(OP.oreSmall.mat(MT.Cu, 1), false));
    }
    @Test void realJeiFocusAdapterFindsOreRecipesAndUsagesAfterAnEarlyEmptyCache() throws Exception {
        var lookup = new WorldgenLookup();
        var material = lookup.getAllRecipes().stream().flatMap(page -> page.materials.stream())
            .filter(candidate -> ST.valid(OP.oreSmall.mat(candidate, 1))).findFirst().orElseThrow();
        var cache = WorldgenLookup.class.getDeclaredField("pages");
        cache.setAccessible(true);
        cache.set(lookup, List.of());
        var stack = OP.oreSmall.mat(material, 1);
        mezz.jei.api.ingredients.ITypedIngredient<ItemStack> ingredient = new mezz.jei.api.ingredients.ITypedIngredient<>() {
            @Override public mezz.jei.api.ingredients.IIngredientType<ItemStack> getType() { return mezz.jei.api.constants.VanillaTypes.ITEM_STACK; }
            @Override public ItemStack getIngredient() { return stack; }
            @Override public mezz.jei.api.ingredients.ITypedIngredient<ItemStack> normalize(mezz.jei.api.ingredients.IIngredientHelper<ItemStack> helper) { return this; }
        };
        var adapter = new mezz.jei.library.load.registration.SingleTypeRecipeManagerPluginAdapter<>(
            (type, focus) -> false, WorldgenCategory.TYPE, lookup);
        for (var role : List.of(mezz.jei.api.recipe.RecipeIngredientRole.INPUT, mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT)) {
            var focus = new mezz.jei.library.focus.Focus<>(role, ingredient);
            assertEquals(List.of(WorldgenCategory.TYPE), adapter.getRecipeTypes(focus));
            assertFalse(adapter.getRecipes(WorldgenCategory.TYPE, focus).isEmpty(), role.toString());
        }
    }
    @Test void previewGridRetainsItemsWhenJeiRemovesOwnedSlotsFromItsLiveList() {
        var slot = (mezz.jei.api.gui.ingredient.IRecipeSlotDrawable) java.lang.reflect.Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{mezz.jei.api.gui.ingredient.IRecipeSlotDrawable.class},
            (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        var live = new java.util.ArrayList<>(List.of(slot));
        var captured = new java.util.concurrent.atomic.AtomicReference<List<mezz.jei.api.gui.ingredient.IRecipeSlotDrawable>>();
        var grid = (mezz.jei.api.gui.widgets.IScrollGridWidget) java.lang.reflect.Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{mezz.jei.api.gui.widgets.IScrollGridWidget.class},
            (proxy, method, args) -> {
                if (method.getName().equals("setPosition")) return proxy;
                throw new AssertionError(method.getName());
            });
        var builder = (mezz.jei.api.gui.widgets.IRecipeExtrasBuilder) java.lang.reflect.Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{mezz.jei.api.gui.widgets.IRecipeExtrasBuilder.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getRecipeSlots" -> (mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView) () -> java.util.Collections.unmodifiableList(live);
                case "addInputHandler", "addWidget" -> null;
                case "addScrollGridWidget" -> {
                    @SuppressWarnings("unchecked") var owned = (List<mezz.jei.api.gui.ingredient.IRecipeSlotDrawable>) args[0];
                    captured.set(owned);
                    live.clear(); // JEI's addSlottedWidget removes the slots from the original list.
                    yield grid;
                }
                default -> throw new AssertionError(method.getName());
            });
        WorldgenCategory.addPreviewGrid(builder);
        assertTrue(live.isEmpty());
        assertEquals(1, captured.get().size());
        assertSame(slot, captured.get().getFirst());
    }
    @Test void configuredCatalogContainsAllGenerationPathsWithRealPreviews() {
        var pages = WorldgenCatalog.collect();
        assertFalse(pages.isEmpty());
        var types = pages.stream().map(page -> page.type).collect(java.util.stream.Collectors.toSet());
        assertTrue(types.containsAll(Set.of("layer", "boundary", "small", "bedrock", "spring", "blob", "coltan", "pit", "sand", "turf")), types.toString());
        for (var page : pages) {
            assertFalse(page.worlds.isEmpty(), page.id);
            assertFalse(page.resources.isEmpty(), page.type + "/" + page.id);
            assertTrue(page.resources.stream().allMatch(stack -> !stack.isEmpty() && stack.getCount() == 1), page.id);
        }
    }
    @Test void hostUsageAndOreMaterialLookupPreserveModernSubtype() {
        var page = new WorldgenCatalog.Page("layer", "test");
        var ore = OP.oreSmall.mat(MT.Fe, 1);
        page.resource("ore", MT.Fe, ore);
        page.host("host", new ItemStack(Items.STONE));
        assertTrue(page.matches(ore, false));
        assertFalse(page.matches(OP.oreSmall.mat(MT.Cu, 1), false));
        assertFalse(page.matches(new ItemStack(Items.STONE), false));
        assertTrue(page.matches(new ItemStack(Items.STONE), true));
    }
    @Test void variantsGroupByRoleAndChanceAndDeduplicateBySubtype() {
        var page = new WorldgenCatalog.Page("boundary", "test");
        var first = OP.oreSmall.mat(MT.Fe, 1);
        var second = OP.oreSmall.mat(MT.Cu, 1);
        page.companion(first, "Ore", "1/24");
        page.companion(first.copyWithCount(3), "Ore", "1/24");
        page.companion(second, "Ore", "1/24");
        page.companion(first, "Ore", "1/48");
        page.host("host_top", new ItemStack(Items.STONE));
        page.host("host_bottom", new ItemStack(Items.STONE));
        assertEquals(5, page.rows.size());
        assertEquals(4, page.previewGroups().size());
        assertEquals(2, page.previewGroups().getFirst().size());
        assertEquals(ST.meta_(second), ST.meta_(page.resources.get(1)));
    }
    @Test void chanceFractionsAndAttemptRangesStayExact() {
        assertEquals("1/24", WorldgenCatalog.reducedChance(151200, 3628800));
        assertEquals("4/21", WorldgenCatalog.reducedChance(691200, 3628800));
        assertEquals("0/1", WorldgenCatalog.reducedChance(0, 3628800));
        for (int amount = 1; amount <= 128; amount++) {
            final int configured = amount;
            var stats = java.util.stream.IntStream.rangeClosed(0, amount)
                .map(roll -> Math.max(1, configured / 2 + roll / 2)).summaryStatistics();
            assertEquals(stats.getMin() + "–" + stats.getMax(), WorldgenCatalog.smallAttempts(amount));
        }
        assertTrue(WorldgenCatalog.sameConditions(24, 80, Set.of("plains", "river"), 24, 80, Set.of("river", "plains")));
        assertFalse(WorldgenCatalog.sameConditions(24, 80, Set.of(), 25, 80, Set.of()));
    }
}
