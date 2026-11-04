package com.plainston.gtqualityneo.integration.worldgen;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.integration.rei.ReiPreviewGrid;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
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
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ReiWorldgenCategory implements DisplayCategory<ReiWorldgenCategory.Card> {
    private static final CategoryIdentifier<Card> ID = CategoryIdentifier.of(GTQualityNeo.MOD_ID, "worldgen");

    public void registerDisplays(DisplayRegistry registry) {
        registry.registerDisplayGenerator(ID, new DynamicDisplayGenerator<>() {
            private List<Card> cards = List.of();

            private synchronized List<Card> cards() {
                if (cards.isEmpty()) cards = WorldgenCatalog.collect().stream().map(Card::new).toList();
                return cards;
            }

            private Optional<List<Card>> find(EntryStack<?> entry, boolean usage) {
                if (entry.getType() == VanillaEntryTypes.ITEM) {
                    ItemStack item = entry.castValue();
                    var query = new WorldgenCatalog.Query(item);
                    return Optional.of(cards().stream().filter(card -> card.page.matches(query, usage)).toList());
                }
                if (entry.getType() == VanillaEntryTypes.FLUID) {
                    var fluid = entry.<dev.architectury.fluid.FluidStack>castValue().getFluid();
                    return Optional.of(cards().stream().filter(card -> card.page.fluid == fluid).toList());
                }
                return Optional.empty();
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
    @Override public Renderer getIcon() { return EntryStacks.of(Blocks.GOLD_ORE); }
    @Override public int getDisplayWidth(Card card) { return 176; }
    @Override public int getDisplayHeight() { return 180; }

    @Override public List<Widget> setupDisplay(Card card, Rectangle bounds) {
        var widgets = new ArrayList<Widget>();
        widgets.add(Widgets.createRecipeBase(bounds));
        int x = bounds.x + 5, y = bounds.y + 4;
        widgets.add(Widgets.createButton(new Rectangle(x, y, 166, 14), text("all_pages"))
            .onClick(button -> ViewSearchBuilder.builder().addCategory(ID).open()));
        widgets.add(Widgets.createLabel(new me.shedaniel.math.Point(x, y + 17), text("type." + card.page.type)).leftAligned());
        ReiPreviewGrid.add(widgets, card.cells, x + 1, y + 31, 8);
        widgets.add(new Details(new Rectangle(x, y + 86, 166, 85), card.page));
        return widgets;
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("gtqualityneo.jei.worldgen." + key, args);
    }

    public static final class Card extends BasicDisplay {
        private final WorldgenCatalog.Page page;
        private final List<ReiPreviewGrid.Cell> cells;

        private Card(WorldgenCatalog.Page page) {
            super(page.hosts.stream().map(stack -> EntryIngredient.of(EntryStacks.of(stack))).toList(),
                page.resources.stream().map(stack -> EntryIngredient.of(EntryStacks.of(stack))).toList());
            this.page = page;
            cells = page.previewGroups().stream().map(group -> {
                var row = group.getFirst();
                boolean host = row.key.endsWith(".host") || row.key.endsWith(".host_top")
                    || row.key.endsWith(".host_bottom") || row.key.endsWith(".flower");
                return new ReiPreviewGrid.Cell(EntryIngredient.of(group.stream()
                    .map(item -> EntryStacks.of(item.stack).tooltip(Component.translatable(row.key, row.arguments))).toList()), !host);
            }).toList();
        }

        @Override public CategoryIdentifier<Card> getCategoryIdentifier() { return ID; }
        @Override public DisplaySerializer<Card> getSerializer() { return null; }
    }

    private static final class Details extends Widget {
        private final Rectangle bounds;
        private final List<FormattedCharSequence> lines = new ArrayList<>();
        private final int visibleLines;
        private int offset;

        Details(Rectangle bounds, WorldgenCatalog.Page page) {
            this.bounds = bounds;
            var minecraft = Minecraft.getInstance();
            var level = minecraft.level;
            var details = new ArrayList<Component>();
            if (page.window != null && level != null) {
                details.add(text("modern_height", level.dimension().identifier().toString(), page.window.height(level)));
                if (page.window.amount() > 0) details.add(text("modern_attempts", page.window.attempts(level)));
            }
            for (var row : page.rows)
                if (row.stack == null) details.add(Component.translatable(row.key, row.arguments));
            details.add(text("configured_rules"));
            details.add(text("source", page.id));
            details.add(text("worlds", page.worlds.stream().map(world -> text("world." + world).getString()).collect(Collectors.joining(", "))));
            for (var detail : details) lines.addAll(minecraft.font.split(detail, bounds.width - 8));
            visibleLines = (bounds.height - 4) / 10;
        }

        @Override public List<? extends GuiEventListener> children() { return List.of(); }
        @Override public boolean containsMouse(double x, double y) { return bounds.contains(x, y); }
        @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
            if (!containsMouse(x, y) || dy == 0 || lines.size() <= visibleLines) return false;
            offset = Math.clamp(offset + (dy > 0 ? -2 : 2), 0, lines.size() - visibleLines);
            return true;
        }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            var font = Minecraft.getInstance().font;
            for (int i = 0; i < visibleLines && offset + i < lines.size(); i++)
                graphics.text(font, lines.get(offset + i), bounds.x + 2, bounds.y + 2 + i * 10, 0xff555555, false);
            if (lines.size() > visibleLines) {
                int height = bounds.height * visibleLines / lines.size();
                int top = bounds.y + (bounds.height - height) * offset / (lines.size() - visibleLines);
                graphics.fill(bounds.getMaxX() - 3, top, bounds.getMaxX(), top + height, 0xff777777);
            }
        }
    }
}
