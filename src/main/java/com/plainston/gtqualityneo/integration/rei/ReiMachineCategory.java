package com.plainston.gtqualityneo.integration.rei;

import com.plainston.gtqualityneo.integration.jei.machine.MachineRecipeVisuals;
import com.plainston.gtqualityneo.integration.jei.machine.RecipeLayout;
import gregapi.recipes.Recipe.RecipeMap;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

record ReiMachineCategory(RecipeMap map, CategoryIdentifier<ReiMachineDisplay> id, RecipeLayout layout)
    implements DisplayCategory<ReiMachineDisplay> {
    ReiMachineCategory(RecipeMap map, CategoryIdentifier<ReiMachineDisplay> id) {
        this(map, id, MachineRecipeVisuals.layout(map));
    }

    @Override public CategoryIdentifier<ReiMachineDisplay> getCategoryIdentifier() { return id; }
    @Override public Component getTitle() { return Component.translatableWithFallback(map.mNameInternal, map.mNameLocal); }
    @Override public Renderer getIcon() {
        return EntryStacks.of(map.mRecipeMachineList.isEmpty() ? ItemStack.EMPTY : map.mRecipeMachineList.getFirst());
    }
    @Override public int getDisplayWidth(ReiMachineDisplay display) { return RecipeLayout.WIDTH; }
    @Override public int getDisplayHeight() { return layout.height(); }

    @Override
    public List<Widget> setupDisplay(ReiMachineDisplay display, Rectangle bounds) {
        var widgets = new ArrayList<Widget>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
            graphics.pose().pushMatrix();
            graphics.pose().translate(bounds.x, bounds.y);
            try { MachineRecipeVisuals.draw(map, display.recipe, graphics); }
            finally { graphics.pose().popMatrix(); }
        }));
        addSlots(widgets, bounds, display.getInputEntries(), false, layout.inputItems(), layout.inputFluids());
        addSlots(widgets, bounds, display.getOutputEntries(), true, layout.outputItems(), layout.outputFluids());
        var special = display.getInputEntries().getLast();
        if (!special.isEmpty()) addSlot(widgets, bounds, layout.special(), special, false);
        int y = bounds.y + layout.infoY();
        for (var line : MachineRecipeVisuals.info(map, display.recipe)) {
            widgets.add(Widgets.createLabel(new Point(bounds.x + 5, y), line).leftAligned());
            y += 10;
        }
        return widgets;
    }

    private void addSlots(List<Widget> widgets, Rectangle bounds, List<EntryIngredient> entries,
                          boolean output, int items, int fluids) {
        for (int i = 0; i < items; i++) addSlot(widgets, bounds, layout.slot(output, false, i), entries.get(i), output);
        for (int i = 0; i < fluids; i++) {
            var fluidsOnly = entries.get(items + i).filter(stack -> stack.getType() == VanillaEntryTypes.FLUID);
            addSlot(widgets, bounds, layout.slot(output, true, i), fluidsOnly, output);
        }
    }

    private static void addSlot(List<Widget> widgets, Rectangle bounds, RecipeLayout.Position position,
                                EntryIngredient entries, boolean output) {
        if (entries.isEmpty()) return;
        var widget = Widgets.createSlot(new Point(bounds.x + position.x() + 1, bounds.y + position.y() + 1))
            .disableBackground().entries(entries);
        widgets.add(output ? widget.markOutput() : widget.markInput());
    }
}
