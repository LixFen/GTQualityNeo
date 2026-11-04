package com.plainston.gtqualityneo.integration.rei;

import com.mojang.logging.LogUtils;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.recipes.AdvancedCrafting1ToY;
import gregapi.recipes.AdvancedCraftingXToY;
import gregapi.recipes.ShapedOreRecipe;
import gregapi.recipes.ShapelessOreRecipe;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class ReiCraftingRecipes {
    private static final SlotDisplay TABLE = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);

    private ReiCraftingRecipes() {}

    static void register(DisplayRegistry registry) {
        var materials = new LinkedHashSet<>(OreDictMaterial.MATERIAL_MAP.values());
        int count = 0;
        for (var recipe : CR.list()) {
            if (recipe instanceof ShapedOreRecipe shaped) {
                var output = shaped.getRecipeOutput();
                var inputs = inputs(shaped.getInput());
                if (ST.valid(output) && !inputs.isEmpty())
                    count += add(registry, new ShapedCraftingRecipeDisplay(shaped.getWidth(), shaped.getHeight(), inputs, slot(output), TABLE));
            } else if (recipe instanceof ShapelessOreRecipe shapeless) {
                var output = shapeless.getRecipeOutput();
                var inputs = inputs(shapeless.getInput().toArray());
                if (ST.valid(output) && !inputs.isEmpty())
                    count += add(registry, new ShapelessCraftingRecipeDisplay(inputs, slot(output), TABLE));
            } else if (recipe instanceof AdvancedCrafting1ToY single && single.mEmpty >= 0 && single.mEmpty < 9) {
                count += addFamily(registry, materials, single.mInput, single.mOutput, single.mOutputCount, single::hasOutputFor, single.mEmpty, 1);
            } else if (recipe instanceof AdvancedCraftingXToY many && many.mInputCount > 0 && many.mInputCount <= 9) {
                count += addFamily(registry, materials, many.mInput, many.mOutput, many.mOutputCount, many::hasOutputFor, -1, many.mInputCount);
            }
        }
        LogUtils.getLogger().info("Registered {} GT6 workbench displays in REI", count);
    }

    private static List<SlotDisplay> inputs(Object[] cells) {
        if (cells == null) return List.of();
        var inputs = new ArrayList<SlotDisplay>(cells.length);
        for (var cell : cells) {
            switch (cell) {
                case null -> inputs.add(SlotDisplay.Empty.INSTANCE);
                case ItemStack stack when ST.valid(stack) -> inputs.add(slot(stack));
                case List<?> choices -> {
                    var alternatives = new ArrayList<SlotDisplay>();
                    for (var choice : choices)
                        if (choice instanceof ItemStack stack && ST.valid(stack)) alternatives.add(slot(stack));
                    if (alternatives.isEmpty()) return List.of();
                    inputs.add(alternatives.size() == 1 ? alternatives.getFirst() : new SlotDisplay.Composite(alternatives));
                }
                default -> { return List.of(); }
            }
        }
        return inputs;
    }

    private static int addFamily(DisplayRegistry registry, Set<OreDictMaterial> materials, OreDictPrefix inputPrefix,
                                 OreDictPrefix outputPrefix, int outputCount, Predicate<OreDictMaterial> hasOutput,
                                 int firstCell, int inputCount) {
        var seen = new HashSet<String>();
        int count = 0;
        for (var material : materials) {
            if (material == null) continue;
            var input = inputPrefix.mat(material, 1);
            if (!ST.valid(input)) continue;
            var data = OM.anydata_(input);
            if (data == null || data.mPrefix != inputPrefix || data.mMaterial == null) continue;
            var actual = data.mMaterial.mMaterial;
            if (actual == null || !hasOutput.test(actual)) continue;
            var output = outputPrefix.mat(actual, outputCount);
            if (!ST.valid(output) || !seen.add(ST.identityKey(input))) continue;

            // Separate cards keep the material pair intact when REI focuses an ingredient.
            if (firstCell >= 0) {
                var cells = new ArrayList<SlotDisplay>(Collections.nCopies(9, SlotDisplay.Empty.INSTANCE));
                cells.set(firstCell, slot(input));
                count += add(registry, new ShapedCraftingRecipeDisplay(3, 3, cells, slot(output), TABLE));
            } else {
                count += add(registry, new ShapelessCraftingRecipeDisplay(Collections.nCopies(inputCount, slot(input)), slot(output), TABLE));
            }
        }
        return count;
    }

    private static SlotDisplay slot(ItemStack stack) {
        return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack));
    }

    private static int add(DisplayRegistry registry, RecipeDisplay recipe) {
        var displays = registry.tryFillDisplay(recipe);
        for (var display : displays) registry.add(display);
        return displays.size();
    }
}
