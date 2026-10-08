package com.plainston.gtqualityneo.integration.worldgen;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IScrollBoxWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class WorldgenCategory extends AbstractRecipeCategory<WorldgenCatalog.Page> {
    public static final IRecipeType<WorldgenCatalog.Page> TYPE = IRecipeType.create(GTQualityNeo.MOD_ID, "worldgen", WorldgenCatalog.Page.class);
    private final Supplier<mezz.jei.api.runtime.IJeiRuntime> runtime;
    public WorldgenCategory(IGuiHelper helper, Supplier<mezz.jei.api.runtime.IJeiRuntime> runtime) {
        super(TYPE, text("title"), helper.createDrawableItemLike(Blocks.GOLD_ORE), 166, 170);
        this.runtime = runtime;
    }
    static Component text(String key, Object... arguments) {
        return Component.translatable("gtqualityneo.jei.worldgen." + key, arguments);
    }
    @Override public Identifier getIdentifier(WorldgenCatalog.Page page) {
        return Identifier.fromNamespaceAndPath(GTQualityNeo.MOD_ID, "worldgen/" + page.type + "/"
            + java.util.HexFormat.of().formatHex(page.id.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, WorldgenCatalog.Page page, IFocusGroup focuses) {
        for (var group : page.previewGroups()) {
            var row = group.getFirst();
            boolean host = row.key.endsWith(".host") || row.key.endsWith(".host_top")
                || row.key.endsWith(".host_bottom") || row.key.endsWith(".flower");
            builder.addSlot(host ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT, 0, 0)
                .addItemStacks(group.stream().map(entry -> entry.stack.copy()).toList())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(row.key, row.arguments)));
        }
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(page.resources);
    }
    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, WorldgenCatalog.Page page, IFocusGroup focuses) {
        builder.addText(text("type." + page.type), 158, 12).setPosition(4, 18);
        addPreviewGrid(builder);
        var scroll = builder.addScrollBoxWidget(158, 70, 4, 96);
        var biomeButton = new BiomeButton(page, scroll, builder);
        builder.addInputHandler(biomeButton);
        biomeButton.refresh();
        builder.addText(text("all_pages"), 158, 14).setPosition(4, 2);
        builder.addInputHandler(new IJeiInputHandler() {
            @Override public ScreenRectangle getArea() { return new ScreenRectangle(4, 0, 158, 16); }
            @Override public boolean handleInput(double x, double y, IJeiUserInput input) {
                if (input.getKey().getType() != InputConstants.Type.MOUSE || input.getKey().getValue() != 0) return false;
                if (!input.isSimulate() && runtime.get() != null) runtime.get().getRecipesGui().showTypes(List.of(TYPE));
                return true;
            }
        });
    }
    static void addPreviewGrid(IRecipeExtrasBuilder builder) {
        // JEI removes these slots from its own list when a slotted widget takes ownership.
        var slots = List.copyOf(builder.getRecipeSlots().getSlots());
        if (!slots.isEmpty()) {
            var cycling = new PreviewCycling();
            builder.addInputHandler(cycling);
            cycling.grid = builder.addScrollGridWidget(slots, 8, 2).setPosition(4, 32);
            builder.addWidget(cycling);
        }
    }
    private static final class PreviewCycling implements IJeiInputHandler, mezz.jei.api.gui.widgets.IRecipeWidget {
        private mezz.jei.api.gui.widgets.IScrollGridWidget grid;
        private final java.util.Set<mezz.jei.api.gui.ingredient.IRecipeSlotDrawable> overridden = new java.util.HashSet<>();
        private int ticks;
        @Override public ScreenRectangle getArea() { return new ScreenRectangle(4, 32, 144, 36); }
        @Override public net.minecraft.client.gui.navigation.ScreenPosition getPosition() {
            return new net.minecraft.client.gui.navigation.ScreenPosition(4, 32);
        }
        @Override public boolean handleMouseScrolled(double x, double y, double dx, double dy) {
            var hovered = grid.getSlotUnderMouse(x, y);
            if (hovered.isEmpty() || dy == 0) return false;
            var slot = hovered.get().slot();
            var items = slot.getItemStacks().toList();
            if (items.size() < 2) return false;
            var current = slot.getDisplayedItemStack().orElse(net.minecraft.world.item.ItemStack.EMPTY);
            int index = 0;
            for (int i = 0; i < items.size(); i++)
                if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(items.get(i), current)) { index = i; break; }
            slot.clearDisplayOverrides();
            slot.createDisplayOverrides().addItemStacks(List.of(items.get(Math.floorMod(index + (dy > 0 ? -1 : 1), items.size()))));
            overridden.add(slot);
            ticks = 20;
            return true;
        }
        @Override public void tick() {
            if (net.minecraft.client.Minecraft.getInstance().hasShiftDown() || ticks == 0) return;
            if (--ticks == 0) {
                overridden.forEach(mezz.jei.api.gui.ingredient.IRecipeSlotDrawable::clearDisplayOverrides);
                overridden.clear();
            }
        }
    }
    private static final class BiomeButton implements IJeiInputHandler, mezz.jei.api.gui.widgets.IRecipeWidget {
        private final WorldgenCatalog.Page page;
        private final IScrollBoxWidget scroll;
        private boolean expanded;
        private final boolean canExpand;
        BiomeButton(WorldgenCatalog.Page page, IScrollBoxWidget scroll, IRecipeExtrasBuilder builder) {
            this.page = page;
            this.scroll = scroll;
            canExpand = page.rows.stream().anyMatch(row -> row.key.endsWith(".biomes")
                && Component.translatable(row.key, row.arguments).getString().length() > 90);
            builder.addWidget(this);
        }
        void refresh() {
            List<FormattedText> lines = new ArrayList<>();
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (page.window != null && level != null) {
                lines.add(text("modern_height", level.dimension().identifier().toString(), page.window.height(level)));
                if (page.window.amount() > 0) lines.add(text("modern_attempts", page.window.attempts(level)));
            }
            for (var row : page.rows) {
                if (row.stack != null) continue;
                Component value = Component.translatable(row.key, row.arguments);
                if (!expanded && canExpand && row.key.endsWith(".biomes"))
                    value = Component.literal(value.getString().substring(0, Math.min(90, value.getString().length())) + "…");
                lines.add(value);
            }
            lines.add(text("configured_rules"));
            lines.add(text("source", page.id));
            lines.add(text("worlds", page.worlds.stream().map(world -> text("world." + world).getString()).collect(Collectors.joining(", "))));
            scroll.setContents(lines);
        }
        @Override public ScreenRectangle getArea() { return new ScreenRectangle(4, 76, 158, 18); }
        @Override public boolean handleInput(double x, double y, IJeiUserInput input) {
            if (!canExpand || input.getKey().getType() != InputConstants.Type.MOUSE || input.getKey().getValue() != 0) return false;
            if (!input.isSimulate()) {
                expanded = !expanded;
                refresh();
            }
            return true;
        }
        @Override public net.minecraft.client.gui.navigation.ScreenPosition getPosition() {
            return new net.minecraft.client.gui.navigation.ScreenPosition(4, 76);
        }
        @Override public void drawWidget(net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            if (canExpand) graphics.text(net.minecraft.client.Minecraft.getInstance().font,
                text(expanded ? "collapse_biomes" : "expand_biomes"), 0, 2, 0xff28536b, false);
        }
    }
}
