package com.plainston.gtqualityneo.integration.rei;

import gregapi.recipes.FurnaceRecipes;
import gregapi.util.ST;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

final class ReiCookingRecipes {
    private ReiCookingRecipes() {}

    static void register(DisplayRegistry registry) {
        add(registry, FurnaceRecipes.smoking(), Items.SMOKER);
        add(registry, FurnaceRecipes.blasting(), Items.BLAST_FURNACE);
    }

    private static void add(DisplayRegistry registry, FurnaceRecipes channel, Item station) {
        var answers = channel.snapshot();
        var seen = ItemStackLinkedSet.createTypeAndComponentsSet();
        for (var candidate : answers.candidates()) {
            var input = ST.amount(1, candidate);
            if (!ST.valid(input) || !seen.add(input)) continue;
            var output = answers.result(input);
            if (!ST.valid(output)) continue;
            var recipe = new FurnaceRecipeDisplay(slot(input), SlotDisplay.AnyFuel.INSTANCE, slot(output),
                new SlotDisplay.ItemSlotDisplay(station), 100, channel.engineExperience(output));
            for (var display : registry.tryFillDisplay(recipe)) registry.add(display);
        }
    }

    private static SlotDisplay slot(ItemStack stack) {
        return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack));
    }
}
