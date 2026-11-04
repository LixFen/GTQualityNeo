package com.plainston.gtqualityneo.integration.rei;

import com.plainston.gtqualityneo.integration.jei.machine.RecipeVariants;
import gregapi.data.FL;
import gregapi.data.LH;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.ST;
import gregapi.util.UT;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.List;

final class ReiMachineDisplay extends BasicDisplay {
    final CategoryIdentifier<ReiMachineDisplay> category;
    final Recipe recipe;

    ReiMachineDisplay(CategoryIdentifier<ReiMachineDisplay> category, RecipeMap map, Recipe recipe) {
        super(inputs(map, recipe), outputs(map, recipe));
        this.category = category;
        this.recipe = recipe;
    }

    @Override public CategoryIdentifier<ReiMachineDisplay> getCategoryIdentifier() { return category; }
    @Override public DisplaySerializer<ReiMachineDisplay> getSerializer() { return null; }

    private static List<EntryIngredient> inputs(RecipeMap map, Recipe recipe) {
        var entries = new ArrayList<EntryIngredient>();
        for (int slot = 0; slot < map.mInputItemsCount; slot++) {
            var options = RecipeVariants.itemOptions(recipe, slot);
            entries.add(EntryIngredient.of(options.stream().map(stack -> {
                if (!ST.zerosize(stack)) return EntryStacks.of(stack.copy());
                return EntryStacks.of(ST.amount(1, stack))
                    .tooltip(Component.literal(LH.tt("Does not get consumed in the process")));
            }).filter(stack -> !stack.isEmpty()).toList()));
        }
        for (int slot = 0; slot < map.mInputFluidCount; slot++)
            entries.add(fluids(RecipeVariants.inputOptions(recipe, slot), map.mUseBucketSizeIn));
        entries.add(recipe.mSpecialItems instanceof ItemStack special && ST.valid(special)
            ? EntryIngredient.of(EntryStacks.of(special.copy())) : EntryIngredient.empty());
        return List.copyOf(entries);
    }

    private static List<EntryIngredient> outputs(RecipeMap map, Recipe recipe) {
        var entries = new ArrayList<EntryIngredient>();
        for (int slot = 0; slot < map.mOutputItemsCount; slot++) {
            var stack = recipe.getOutput(slot);
            if (stack == null || stack.isEmpty()) {
                entries.add(EntryIngredient.empty());
                continue;
            }
            var entry = EntryStacks.of(stack);
            int chance = recipe.getOutputChance(slot), maximum = recipe.getMaxChance(slot);
            if (chance > 0 && chance != maximum) {
                long percent = UT.Code.units(chance, maximum, 10000, false);
                entry.tooltip(Component.translatable(stack.getCount() > 1
                    ? "gtqualityneo.jei.machine.chance_each" : "gtqualityneo.jei.machine.chance",
                    "%d.%02d%%".formatted(percent / 100, percent % 100)));
            }
            entries.add(EntryIngredient.of(entry));
        }
        for (int slot = 0; slot < map.mOutputFluidCount; slot++)
            entries.add(slot < recipe.mFluidOutputs.length && recipe.mFluidOutputs[slot] != null
                ? fluids(List.of(recipe.mFluidOutputs[slot]), map.mUseBucketSizeOut) : EntryIngredient.empty());
        return List.copyOf(entries);
    }

    private static EntryIngredient fluids(List<FluidStack> fluids, boolean bucketScale) {
        var entries = EntryIngredient.builder();
        for (var fluid : fluids) {
            if (fluid.isEmpty()) continue;
            var nativeStack = dev.architectury.fluid.FluidStack.create(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch());
            var entry = EntryStacks.of(nativeStack);
            entries.add(entry);
            var icon = FL.display(fluid, true, false, bucketScale);
            if (icon != null && !icon.isEmpty()) {
                var iconEntry = EntryStacks.of(icon);
                entry.withRenderer(new FluidIconRenderer(iconEntry));
                entries.add(iconEntry);
            }
        }
        return entries.build();
    }

    /** Draws the GT6 icon and tooltip while the slot retains its native fluid identity. */
    private record FluidIconRenderer(EntryStack<ItemStack> icon) implements EntryRenderer<dev.architectury.fluid.FluidStack> {
        @Override
        public void render(EntryStack<dev.architectury.fluid.FluidStack> entry, GuiGraphics graphics,
                           Rectangle bounds, int mouseX, int mouseY, float delta) {
            icon.render(graphics, bounds, mouseX, mouseY, delta);
        }

        @Override
        public Tooltip getTooltip(EntryStack<dev.architectury.fluid.FluidStack> entry, TooltipContext context) {
            return icon.getTooltip(context, false);
        }
    }
}
