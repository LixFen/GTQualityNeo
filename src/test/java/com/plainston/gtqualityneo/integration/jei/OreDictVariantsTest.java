package com.plainston.gtqualityneo.integration.jei;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.recipes.Recipe;
import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EphemeralTestServerProvider.class)
class OreDictVariantsTest {
    static final RecipeVariants.Mode ITEMS = new RecipeVariants.Mode(false, true);

    @org.junit.jupiter.api.BeforeAll static void initialize(net.minecraft.server.MinecraftServer server) throws Exception {
        if (server.overworld() != null) return;
        server.getWorldData().overworldData().setInitialized(true);
        var createLevels = net.minecraft.server.MinecraftServer.class.getDeclaredMethod("createLevels");
        createLevels.setAccessible(true);
        server.submit(() -> {
            try { createLevels.invoke(server); }
            catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
        }).get();
    }

    static Recipe bath(OreDictMaterial input, OreDictMaterial output, FluidStack fluid) {
        var in = OP.plate.mat(input, 1);
        var out = OP.plate.mat(output, 1);
        assertTrue(ST.valid(in) && ST.valid(out), input.mNameInternal + " plate exists");
        return new Recipe(false, false, new ItemStack[]{in}, new ItemStack[]{out}, null, new long[]{10000},
            new FluidStack[]{fluid}, null, 144, 0, 0);
    }

    static FluidStack zinc() { return new FluidStack(Fluids.WATER, 16); }

    @Test void anySteelPlatesGalvanizedByZincShareOneCard() {
        var steel = bath(MT.Steel, MT.SteelGalvanized, zinc());
        var knight = bath(MT.Knightmetal, MT.SteelGalvanized, zinc());
        var meteoric = bath(MT.MeteoricSteel, MT.SteelGalvanized, zinc());
        var pages = RecipeVariants.group(List.of(steel, knight, meteoric), RecipeVariants.Mode.ALL);
        assertEquals(1, pages.size());
        var options = RecipeVariants.itemOptions(pages.getFirst(), 0);
        assertEquals(3, options.size());
        assertTrue(ItemStack.isSameItemSameComponents(options.get(1), knight.mInputs[0]));
        assertEquals(1, RecipeVariants.inputOptions(pages.getFirst(), 0).size(), "The shared zinc is shown once");
        assertEquals(3, RecipeVariants.group(List.of(steel, knight, meteoric), RecipeVariants.Mode.FLUIDS).size(),
            "The ore-dictionary switch alone controls item merging");
    }

    @Test void outputsThatFollowTheInputMaterialAndUnrelatedMaterialsStaySeparate() {
        var steelFoil = bath(MT.Steel, MT.Steel, zinc());
        var knightFoil = bath(MT.Knightmetal, MT.Knightmetal, zinc());
        assertEquals(2, RecipeVariants.group(List.of(steelFoil, knightFoil), RecipeVariants.Mode.ALL).size());
        var copper = bath(MT.Cu, MT.SteelGalvanized, zinc());
        var steel = bath(MT.Steel, MT.SteelGalvanized, zinc());
        assertEquals(2, RecipeVariants.group(List.of(copper, steel), RecipeVariants.Mode.ALL).size(),
            "Copper is not in any family with steel");
    }

    @Test void completeMaterialAndFluidProductShowsEachOptionOnce() {
        var water = new FluidStack(Fluids.WATER, 144);
        var lava = new FluidStack(Fluids.LAVA, 144);
        var members = List.of(bath(MT.Steel, MT.FierySteel, water), bath(MT.Steel, MT.FierySteel, lava),
            bath(MT.Knightmetal, MT.FierySteel, water), bath(MT.Knightmetal, MT.FierySteel, lava));
        var pages = RecipeVariants.group(members, RecipeVariants.Mode.ALL);
        assertEquals(1, pages.size());
        var items = RecipeVariants.itemOptions(pages.getFirst(), 0);
        var fluids = RecipeVariants.inputOptions(pages.getFirst(), 0);
        assertEquals(2, items.size());
        assertTrue(ItemStack.isSameItemSameComponents(items.get(0), members.get(0).mInputs[0]));
        assertTrue(ItemStack.isSameItemSameComponents(items.get(1), members.get(2).mInputs[0]));
        assertEquals(List.of(Fluids.WATER, Fluids.LAVA), fluids.stream().map(FluidStack::getFluid).toList());
        assertEquals(2, RecipeVariants.group(members, RecipeVariants.Mode.FLUIDS).size(), "One card per steel");
        assertEquals(2, RecipeVariants.group(members, ITEMS).size(), "One card per fluid");
        assertEquals(4, RecipeVariants.group(members, new RecipeVariants.Mode(false, false)).size());
    }

    @Test void incompleteCombinationsSplitIntoCompleteCards() {
        var water = new FluidStack(Fluids.WATER, 144);
        var lava = new FluidStack(Fluids.LAVA, 144);
        // Steel and Meteoric Steel take both fluids, Knightmetal only water: one 2x2 card and one single recipe.
        var knightWater = bath(MT.Knightmetal, MT.FierySteel, water);
        var members = List.of(bath(MT.Steel, MT.FierySteel, water), bath(MT.Steel, MT.FierySteel, lava),
            knightWater, bath(MT.MeteoricSteel, MT.FierySteel, water), bath(MT.MeteoricSteel, MT.FierySteel, lava));
        var pages = RecipeVariants.group(members, RecipeVariants.Mode.ALL);
        assertEquals(2, pages.size());
        assertEquals(2, RecipeVariants.itemOptions(pages.get(0), 0).size());
        assertEquals(2, RecipeVariants.inputOptions(pages.get(0), 0).size());
        assertSame(knightWater, pages.get(1));
        for (var page : pages) {
            var shown = RecipeVariants.itemOptions(page, 0).size() * RecipeVariants.inputOptions(page, 0).size();
            int real = page instanceof RecipeVariants.Grouped grouped ? grouped.members().size() : 1;
            assertEquals(real, shown, "Every combination the card suggests is registered");
        }
        var registry = new RegisteredRecipeVariants();
        members.forEach(registry::add);
        assertSame(knightWater, registry.pageFor(knightWater, RecipeVariants.Mode.ALL));
        assertEquals(2, registry.pages(RecipeVariants.Mode.ALL).size());
    }

    @Test void registryRegroupsWhenTheModeChangesAndReusesPagesOtherwise() {
        var registry = new RegisteredRecipeVariants();
        var steel = bath(MT.Steel, MT.SteelGalvanized, zinc());
        var knight = bath(MT.Knightmetal, MT.SteelGalvanized, zinc());
        registry.add(steel);
        registry.add(knight);
        assertEquals(1, registry.groupCount());
        var merged = registry.pageFor(steel, RecipeVariants.Mode.ALL);
        assertSame(merged, registry.pageFor(knight, RecipeVariants.Mode.ALL));
        assertSame(merged, registry.pages(RecipeVariants.Mode.ALL).getFirst());
        assertEquals(List.of(steel, knight), registry.pages(RecipeVariants.Mode.FLUIDS));
        assertSame(steel, registry.pageFor(steel, RecipeVariants.Mode.FLUIDS));
        registry.remove(knight);
        assertSame(steel, registry.pageFor(steel, RecipeVariants.Mode.ALL));
    }

    @Test void gt6GeneratedGalvanizingRecipesMergeIntoOneCard() {
        var produced = gregapi.data.RM.Bath.getNEIRecipesFor(OP.plate.mat(MT.SteelGalvanized, 1)).stream()
            .filter(recipe -> recipe.mFluidInputs.length == 1 && recipe.mFluidInputs[0] != null
                && recipe.mFluidInputs[0].getFluid() == MT.Zn.liquid(1, true).getFluid()).toList();
        assertEquals(gregapi.data.ANY.Steel.mToThis.size(), produced.size(),
            "GT6 makes one zinc bath recipe per AnySteel material: " + produced.stream().map(r -> r.mInputs[0]).toList());
        var pages = RecipeVariants.group(produced, RecipeVariants.Mode.ALL);
        assertEquals(1, pages.size());
        assertEquals(produced.size(), RecipeVariants.itemOptions(pages.getFirst(), 0).size());
    }

    @Test void renamedOrOtherwiseModifiedStacksAreNotTreatedAsMaterialItems() {
        var steel = bath(MT.Steel, MT.SteelGalvanized, zinc());
        var knight = bath(MT.Knightmetal, MT.SteelGalvanized, zinc());
        knight.mInputs[0].set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
            net.minecraft.network.chat.Component.literal("special plate"));
        assertEquals(2, RecipeVariants.group(List.of(steel, knight), RecipeVariants.Mode.ALL).size());
    }
}
