package com.plainston.gtqualityneo.integration.multiblock;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.integration.rei.ReiPreviewGrid;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static com.plainston.gtqualityneo.integration.multiblock.StructureScene.text;

public final class ReiMultiblockCategory implements DisplayCategory<ReiMultiblockCategory.Card> {
    private static final CategoryIdentifier<Card> ID = CategoryIdentifier.of(GTQualityNeo.MOD_ID, "multiblock");

    public void registerDisplays(DisplayRegistry registry) {
        registry.registerDisplayGenerator(ID, new DynamicDisplayGenerator<>() {
            private RegistryAccess registries;
            private List<Card> cards = List.of();

            private synchronized List<Card> cards() {
                var level = Minecraft.getInstance().level;
                if (level == null) return List.of();
                if (registries != level.registryAccess()) {
                    var access = level.registryAccess();
                    cards = MultiblockCatalog.collect(access).stream().map(Card::new).toList();
                    registries = access;
                }
                return cards;
            }

            private Optional<List<Card>> find(EntryStack<?> entry, boolean usage) {
                if (entry.getType() != VanillaEntryTypes.ITEM) return Optional.empty();
                ItemStack item = entry.castValue();
                return Optional.of(cards().stream().filter(card -> ItemStack.isSameItemSameComponents(item, card.page.controllerStack())
                    || usage && card.page.ingredients.stream().anyMatch(part -> ItemStack.isSameItemSameComponents(item, part))).toList());
            }

            @Override public Optional<List<Card>> getRecipeFor(EntryStack<?> entry) { return find(entry, false); }
            @Override public Optional<List<Card>> getUsageFor(EntryStack<?> entry) { return find(entry, true); }
            @Override public Optional<List<Card>> generate(ViewSearchBuilder builder) {
                return builder.getCategories().contains(ID) ? Optional.of(cards()) : Optional.empty();
            }
        });
    }

    @Override public CategoryIdentifier<Card> getCategoryIdentifier() { return ID; }
    @Override public Component getTitle() { return text("title"); }
    @Override public Renderer getIcon() { return EntryStacks.of(Blocks.BRICKS); }
    @Override public int getDisplayWidth(Card card) { return 204; }
    @Override public int getDisplayHeight() { return 213; }

    @Override public List<Widget> setupDisplay(Card card, Rectangle bounds) {
        var widgets = new ArrayList<Widget>();
        widgets.add(Widgets.createRecipeBase(bounds));
        int x = bounds.x + 2, y = bounds.y + 2;
        var scene = new SceneWidget(card.page, x, y);
        widgets.add(scene);
        widgets.add(Widgets.createSlot(new Point(x + 181, y + 1)).entries(card.getOutputEntries().getFirst()).markOutput());
        var layer = Widgets.createButton(new Rectangle(x + 3, y + 142, 105, 14), scene.preview.layerLabel());
        layer.onClick(button -> scene.preview.nextLayer());
        // Shift-scroll and reset can also change the label.
        layer.onRender((graphics, button) -> button.setText(scene.preview.layerLabel()));
        widgets.add(Widgets.withTooltip(layer, text("layer_cycle")));
        widgets.add(Widgets.withTooltip(Widgets.createButton(new Rectangle(x + 113, y + 142, 24, 14), Component.literal("−"))
            .onClick(button -> scene.preview.zoom(false)), text("zoom_out")));
        widgets.add(Widgets.withTooltip(Widgets.createButton(new Rectangle(x + 141, y + 142, 24, 14), Component.literal("+"))
            .onClick(button -> scene.preview.zoom(true)), text("zoom_in")));
        widgets.add(Widgets.withTooltip(Widgets.createButton(new Rectangle(x + 169, y + 142, 26, 14), Component.literal("↺"))
            .onClick(button -> scene.preview.reset()), text("reset")));
        ReiPreviewGrid.add(widgets, card.getInputEntries().stream().map(entries -> new ReiPreviewGrid.Cell(entries, false)).toList(),
            x + 5, y + 161, 10);
        return widgets;
    }

    public static final class Card extends BasicDisplay {
        private final MultiblockCatalog.Page page;

        private Card(MultiblockCatalog.Page page) {
            super(page.ingredients.stream().map(stack -> EntryIngredient.of(EntryStacks.of(stack))).toList(),
                List.of(EntryIngredient.of(EntryStacks.of(page.controllerStack()))));
            this.page = page;
        }

        @Override public CategoryIdentifier<Card> getCategoryIdentifier() { return ID; }
        @Override public DisplaySerializer<Card> getSerializer() { return null; }
    }

    private static final class SceneWidget extends Widget {
        private final MultiblockCatalog.Page page;
        private final StructureScene preview;
        private final int x, y;
        private boolean dragged;

        SceneWidget(MultiblockCatalog.Page page, int x, int y) {
            this.page = page;
            this.preview = new StructureScene(page);
            this.x = x;
            this.y = y;
        }

        @Override public List<? extends GuiEventListener> children() { return List.of(); }
        @Override public boolean containsMouse(double mx, double my) {
            return mx >= x && mx < x + 200 && my >= y + 18 && my < y + 140;
        }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            try { preview.draw(graphics); }
            finally { graphics.pose().popMatrix(); }
            var pos = preview.hit(mouseX - x, mouseY - y);
            if (pos != null) Tooltip.create(new Point(mouseX, mouseY), page.stackAt(pos).getHoverName()).queue();
        }
        @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!containsMouse(event.x(), event.y()) || event.button() > 1) return false;
            dragged = false;
            return true;
        }
        @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            if (event.button() != 0) return false;
            dragged = true;
            preview.rotate(dx, dy);
            return true;
        }
        @Override public boolean mouseReleased(MouseButtonEvent event) {
            if (dragged || !containsMouse(event.x(), event.y()) || event.button() > 1) return false;
            var pos = preview.hit(event.x() - x, event.y() - y);
            if (pos == null) return false;
            var stack = EntryStacks.of(page.stackAt(pos));
            var view = ViewSearchBuilder.builder();
            return (event.button() == 0 ? view.addRecipesFor(stack) : view.addUsagesFor(stack)).open();
        }
        @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
            if (dy == 0 || !containsMouse(mx, my)) return false;
            preview.scroll(dy, Minecraft.getInstance().hasShiftDown());
            return true;
        }
    }
}
