package com.plainston.gtqualityneo.integration.jei;

import static org.junit.jupiter.api.Assertions.*;
import gregapi.data.RM;
import gregapi.data.IL;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.gui.ContainerCommonBasicMachine;
import gregapi.gui.Slot_Render;
import gregapi.tileentity.machines.MultiTileEntityBasicMachine;
import gregapi.util.ST;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(EphemeralTestServerProvider.class)
class MachineRecipeDisplayTest {
    private static MinecraftServer server;
    @BeforeAll static void initialize(MinecraftServer server) throws Exception {
        MachineRecipeDisplayTest.server = server;
        if (server.overworld() == null) {
            server.getWorldData().overworldData().setInitialized(true);
            var createLevels = MinecraftServer.class.getDeclaredMethod("createLevels");
            createLevels.setAccessible(true);
            server.submit(() -> {
                try { createLevels.invoke(server); }
                catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
            }).get();
        }
    }

    @Test void retainsCatalystsOutputChancesFluidDetailsAndIngredientRoles() {
        var catalyst = new ItemStack(Items.IRON_INGOT);
        ST.size_(0, catalyst);
        var recipe = new Recipe(false, false, new ItemStack[]{catalyst},
            new ItemStack[]{new ItemStack(Items.GOLD_INGOT, 2)}, new ItemStack(Items.DIAMOND), new long[]{2500},
            new FluidStack[]{new FluidStack(Fluids.WATER, 1000)},
            new FluidStack[]{new FluidStack(Fluids.LAVA, 500)}, 200, 32, 0);
        var before = recipe.mInputs[0].copy();
        var recorder = new Recorder();
        MachineRecipeDisplay.setRecipe(RM.Electrolyzer, recorder.builder(), recipe);
        var input = recorder.slots.get("input/item/0");
        var output = recorder.slots.get("output/item/0");
        assertEquals(RecipeIngredientRole.INPUT, input.role);
        assertEquals(RecipeIngredientRole.OUTPUT, output.role);
        assertEquals(1, input.callbacks.size(), "Zero-consumption input retains its tooltip");
        assertEquals(1, output.callbacks.size(), "Chanced output retains its tooltip");
        assertTrue(output.items.getFirst().is(Items.GOLD_INGOT));
        assertEquals(2, output.items.getFirst().getCount());
        assertTrue(IL.Display_Fluid.equal(recorder.slots.get("input/fluid/0").items.getFirst(), true, true));
        assertTrue(IL.Display_Fluid.equal(recorder.slots.get("output/fluid/0").items.getFirst(), true, true));
        assertEquals(List.of(1000, 500), recorder.nativeFluids.stream().map(FluidStack::getAmount).toList());
        assertTrue(recorder.slots.get("special").items.getFirst().is(Items.DIAMOND));
        assertFalse(recorder.slots.containsKey("machine"));
        assertTrue(ItemStack.matches(before, recipe.mInputs[0]));
        assertEquals(0, ST.count(recipe.mInputs[0]));
        assertEquals(1000, recipe.mFluidInputs[0].getAmount());
        var info = MachineRecipeDisplay.info(RM.Electrolyzer, recipe).stream().map(value -> value.getString()).toList();
        assertTrue(info.stream().anyMatch(value -> value.contains("200")));
        assertTrue(info.stream().anyMatch(value -> value.contains("6,400") || value.contains("6400")));
    }

    @Test void generatedRecipesAndEmptyIngredientArraysAreSupported() {
        var recipe = new Recipe(false, false, null, null, null, null, null, null, 40, -8, 0);
        var recorder = new Recorder();
        MachineRecipeDisplay.setRecipe(RM.Mixer, recorder.builder(), recipe);
        assertEquals(RM.Mixer.mInputItemsCount + RM.Mixer.mOutputItemsCount
            + RM.Mixer.mInputFluidCount + RM.Mixer.mOutputFluidCount, recorder.slots.size());
        assertFalse(recorder.slots.containsKey("machine"));
        assertTrue(recorder.slots.values().stream().filter(slot -> slot.role == RecipeIngredientRole.OUTPUT)
            .allMatch(slot -> slot.items.isEmpty()));
        assertFalse(MachineRecipeDisplay.info(RM.Mixer, recipe).isEmpty());
    }

    @Test void equivalentFluidOptionsAreVisibleAndSearchable() {
        var water = MachineRecipeLookupTest.recipe(new FluidStack(Fluids.WATER, 100));
        var lava = MachineRecipeLookupTest.recipe(new FluidStack(Fluids.LAVA, 100));
        var page = RecipeVariants.group(List.of(water, lava), RecipeVariants.Mode.ALL).getFirst();
        var recorder = new Recorder();
        MachineRecipeDisplay.setRecipe(RM.Bath, recorder.builder(), page);
        assertEquals(2, recorder.slots.get("input/fluid/0").items.size());
        assertEquals(2, recorder.nativeFluids.size());
        assertTrue(recorder.nativeFluids.stream().anyMatch(fluid -> fluid.getFluid() == Fluids.WATER));
        assertTrue(recorder.nativeFluids.stream().anyMatch(fluid -> fluid.getFluid() == Fluids.LAVA));
        assertTrue(recorder.nativeFluids.stream().allMatch(fluid -> fluid.getAmount() == 100));
        assertTrue(recorder.links.isEmpty(), "A single varying slot needs no link");
    }

    @Test void materialAndFluidVariantsAreEachShownOnce() {
        var water = new FluidStack(Fluids.WATER, 144);
        var lava = new FluidStack(Fluids.LAVA, 144);
        var members = List.of(OreDictVariantsTest.bath(gregapi.data.MT.Steel, gregapi.data.MT.FierySteel, water),
            OreDictVariantsTest.bath(gregapi.data.MT.Steel, gregapi.data.MT.FierySteel, lava),
            OreDictVariantsTest.bath(gregapi.data.MT.Knightmetal, gregapi.data.MT.FierySteel, water),
            OreDictVariantsTest.bath(gregapi.data.MT.Knightmetal, gregapi.data.MT.FierySteel, lava));
        var page = RecipeVariants.group(members, RecipeVariants.Mode.ALL).getFirst();
        var recorder = new Recorder();
        MachineRecipeDisplay.setRecipe(RM.Bath, recorder.builder(), page);
        assertEquals(2, recorder.slots.get("input/item/0").items.size());
        assertEquals(2, recorder.slots.get("input/fluid/0").items.size());
        assertEquals(2, recorder.nativeFluids.size());
        assertEquals(1, recorder.slots.get("output/item/0").items.size());
        assertTrue(recorder.links.isEmpty(), "Slots of a complete product cycle independently");
    }

    @Test void textureCropsMatchTheOriginalMachineMenuCoordinates() {
        var player = FakePlayerFactory.getMinecraft(server.overworld());
        for (var map : RecipeMap.RECIPE_MAP_LIST) {
            if (!map.mNEIAllowed || map.mInputItemsCount > 12 || map.mOutputItemsCount > 12) continue;
            var machine = new MultiTileEntityBasicMachine();
            machine.mRecipes = map;
            machine.readFromNBT2(new CompoundTag());
            machine.setLevel(server.overworld());
            var menu = new ContainerCommonBasicMachine(player.getInventory(), machine, map, 0);
            try {
                int special = map.mInputItemsCount + map.mOutputItemsCount;
                for (var slot : menu.slots) {
                    if (slot.container != machine) continue;
                    int index = slot.getSlotIndex();
                    RecipeTextures.Source source;
                    if (slot instanceof Slot_Render) {
                        int fluid = index - special - 1;
                        boolean output = fluid >= map.mInputFluidCount;
                        source = RecipeTextures.fluid(output, output ? fluid - map.mInputFluidCount : fluid);
                    } else if (index == special) source = new RecipeTextures.Source(79, 42);
                    else {
                        boolean output = index >= map.mInputItemsCount;
                        source = RecipeTextures.item(output, output ? map.mOutputItemsCount : map.mInputItemsCount,
                            output ? map.mOutputFluidCount : map.mInputFluidCount,
                            output ? index - map.mInputItemsCount : index);
                    }
                    assertEquals(new RecipeTextures.Source(slot.x - 1, slot.y - 1), source,
                        map.mNameInternal + " slot " + index);
                }
            } finally { menu.removed(player); }
        }
    }

    @Test void informationAreaShrinksToTheMapsTextRequirements() {
        var layout = MachineRecipeDisplay.layout(RM.Electrolyzer);
        assertTrue(layout.infoHeight() < 84);
        assertEquals(layout.infoY() + layout.infoHeight(), layout.height());
    }

    private static final class CapturedSlot {
        final RecipeIngredientRole role;
        final List<ItemStack> items = new ArrayList<>();
        final List<IRecipeSlotRichTooltipCallback> callbacks = new ArrayList<>();
        CapturedSlot(RecipeIngredientRole role) { this.role = role; }
    }

    private static final class Recorder {
        final Map<String, CapturedSlot> slots = new HashMap<>();
        final List<FluidStack> nativeFluids = new ArrayList<>();
        final List<List<?>> links = new ArrayList<>();

        IRecipeLayoutBuilder builder() {
            return (IRecipeLayoutBuilder) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IRecipeLayoutBuilder.class}, (proxy, method, args) -> {
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    if (method.getName().equals("addSlot")) return slot((RecipeIngredientRole) args[0], false);
                    if (method.getName().equals("addInvisibleIngredients")) return slot((RecipeIngredientRole) args[0], true);
                    if (method.getName().equals("createFocusLink")) { links.add(List.copyOf((java.util.Collection<?>) args[0])); return null; }
                    throw new AssertionError(method.getName());
                });
        }

        Object slot(RecipeIngredientRole role, boolean invisible) {
            var captured = new CapturedSlot(role);
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IRecipeSlotBuilder.class},
                (proxy, method, args) -> {
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    switch (method.getName()) {
                        case "setSlotName" -> slots.put((String) args[0], captured);
                        case "addRichTooltipCallback" -> captured.callbacks.add((IRecipeSlotRichTooltipCallback) args[0]);
                        case "addItemStacks" -> captured.items.addAll((List<ItemStack>) args[0]);
                        case "add" -> {
                            if (args.length == 1 && args[0] instanceof ItemStack stack) captured.items.add(stack);
                            if (invisible && args.length == 2 && args[1] instanceof FluidStack fluid) nativeFluids.add(fluid);
                        }
                        case "setPosition", "setStandardSlotBackground" -> { }
                        default -> throw new AssertionError(method.getName());
                    }
                    return proxy;
                });
        }
    }
}
