package com.plainston.gtqualityneo.integration.jei.crafting;

import com.plainston.gtqualityneo.integration.RecipeMaterialVisibility;
import gregapi.recipes.GT6CraftingDisplay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import java.util.ArrayList;
import java.util.List;

final class VisibleCraftingRecipe {
    private VisibleCraftingRecipe() {}

    static RecipeHolder<CraftingRecipe> filter(RecipeHolder<CraftingRecipe> holder) {
        var display = holder.value().display().getFirst();
        boolean shapeless = display instanceof ShapelessCraftingRecipeDisplay;
        var inputs = shapeless ? ((ShapelessCraftingRecipeDisplay) display).ingredients()
            : ((ShapedCraftingRecipeDisplay) display).ingredients();
        var output = ((SlotDisplay.ItemStackSlotDisplay) display.result()).stack().create();
        if (RecipeMaterialVisibility.hidden(output)) return null;
        var cells = new ArrayList<List<ItemStack>>();
        boolean changed = false;
        for (var input : inputs) {
            var choices = stacks(input);
            var visible = choices.stream().filter(stack -> !RecipeMaterialVisibility.hidden(stack)).toList();
            if (!choices.isEmpty() && visible.isEmpty()) return null;
            changed |= choices.size() != visible.size();
            cells.add(visible);
        }
        if (!changed) return holder;
        int width = shapeless ? 0 : ((ShapedCraftingRecipeDisplay) display).width();
        int height = shapeless ? 0 : ((ShapedCraftingRecipeDisplay) display).height();
        return new RecipeHolder<>(holder.id(), new GT6CraftingDisplay(shapeless, width, height, cells, output));
    }

    static List<ItemStack> stacks(SlotDisplay slot) {
        if (slot instanceof SlotDisplay.ItemStackSlotDisplay stack) return List.of(stack.stack().create());
        if (slot instanceof SlotDisplay.Composite composite)
            return composite.contents().stream().flatMap(part -> stacks(part).stream()).toList();
        return List.of();
    }
}
