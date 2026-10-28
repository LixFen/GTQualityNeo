package com.plainston.gtqualityneo.integration.multiblock;

import com.mojang.blaze3d.platform.InputConstants;
import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.mixin.client.GuiGraphicsAccessor;
import gregapi.render.GT6BlockModel;
import gregapi.render.GT6QuadBuilder;
import gregapi.render.IRenderedBlockObject;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class MultiblockCategory extends AbstractRecipeCategory<MultiblockCatalog.Page> {
    public static final IRecipeType<MultiblockCatalog.Page> TYPE = IRecipeType.create(GTQualityNeo.MOD_ID, "multiblock", MultiblockCatalog.Page.class);
    private final IJeiHelpers helpers;
    private final Supplier<IJeiRuntime> runtime;
    public MultiblockCategory(IJeiHelpers helpers, Supplier<IJeiRuntime> runtime) {
        super(TYPE, text("title"), helpers.getGuiHelper().createDrawableItemLike(Blocks.BRICKS), 200, 196);
        this.helpers = helpers; this.runtime = runtime;
    }
    static Component text(String key, Object... args) { return Component.translatable("gtqualityneo.jei.multiblock." + key, args); }
    @Override public Identifier getIdentifier(MultiblockCatalog.Page page) {
        return Identifier.fromNamespaceAndPath(GTQualityNeo.MOD_ID, "multiblock/" + page.id);
    }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, MultiblockCatalog.Page page, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 180, 0).addItemStacks(List.of(page.controllerStack()));
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(List.of(page.controllerStack()));
        for (var stack : page.ingredients) builder.addSlot(RecipeIngredientRole.INPUT, 0, 0).addItemStacks(List.of(stack));
    }
    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, MultiblockCatalog.Page page, IFocusGroup focuses) {
        var scene = new Scene(page);
        builder.addWidget(scene);
        builder.addInputHandler(scene);
        addControl(builder, 4, 106, scene::layerLabel, text("layer_cycle"), scene::nextLayer);
        addControl(builder, 114, 24, () -> Component.literal("−"), text("zoom_out"),
            () -> scene.zoom = Math.max(.35f, scene.zoom / 1.15f));
        addControl(builder, 142, 24, () -> Component.literal("+"), text("zoom_in"),
            () -> scene.zoom = Math.min(4, scene.zoom * 1.15f));
        addControl(builder, 170, 26, () -> Component.literal("↺"), text("reset"), scene::reset);
        var slots = builder.getRecipeSlots().getSlots().stream().filter(slot -> slot.getRole() == RecipeIngredientRole.INPUT).toList();
        if (!slots.isEmpty()) builder.addScrollGridWidget(slots, 10, 2).setPosition(4, 157);
    }

    private static void addControl(IRecipeExtrasBuilder builder, int x, int width, Supplier<Component> label,
        Component tooltip, Runnable action) {
        var control = new ControlButton(x, width, label, tooltip, action);
        builder.addWidget(control);
        builder.addInputHandler(control);
    }

    private static final class ControlButton implements IRecipeWidget, IJeiInputHandler {
        private final int x;
        private final Supplier<Component> label;
        private final Component tooltip;
        private final Button button;
        ControlButton(int x, int width, Supplier<Component> label, Component tooltip, Runnable action) {
            this.x = x; this.label = label; this.tooltip = tooltip;
            button = Button.builder(label.get(), ignored -> action.run()).bounds(0, 0, width, 14).build();
        }
        @Override public ScreenPosition getPosition() { return new ScreenPosition(x, 141); }
        @Override public ScreenRectangle getArea() { return new ScreenRectangle(x, 141, button.getWidth(), button.getHeight()); }
        @Override public ScreenRectangle getScreenRectangle() { return getArea(); }
        @Override public void drawWidget(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            button.setMessage(label.get());
            button.extractRenderState(graphics, (int) mouseX, (int) mouseY, 0);
        }
        @Override public void getTooltip(ITooltipBuilder builder, double mouseX, double mouseY) {
            if (button.isMouseOver(mouseX, mouseY)) builder.add(tooltip);
        }
        @Override public boolean handleInput(double x, double y, IJeiUserInput input) {
            if (input.getKey().getType() != InputConstants.Type.MOUSE || input.getKey().getValue() != 0
                || !button.isMouseOver(x, y)) return false;
            if (!input.isSimulate()) {
                button.playDownSound(Minecraft.getInstance().getSoundManager());
                button.onPress(new MouseButtonInfo(0, 0));
            }
            return true;
        }
    }

    private final class Scene implements IRecipeWidget, IJeiInputHandler {
        private final MultiblockCatalog.Page page;
        private final Map<Integer, List<StructureRenderState.Cell>> meshes = new HashMap<>();
        private final Vector3f center;
        private final int minY, maxY;
        private final float fit;
        private float yaw, pitch = 25, zoom = 1;
        private int layer = -1;
        Scene(MultiblockCatalog.Page page) {
            this.page = page;
            yaw = StructureRenderer.frontYaw(page.facing);
            var positions = new ArrayList<BlockPos>();
            positions.add(new BlockPos(0, MultiblockCatalog.Y, 0));
            for (var cell : page.parts) { var p = cell.position(); positions.add(new BlockPos(p.x(), p.y(), p.z())); }
            int minX = positions.stream().mapToInt(BlockPos::getX).min().orElse(0), maxX = positions.stream().mapToInt(BlockPos::getX).max().orElse(0);
            minY = positions.stream().mapToInt(BlockPos::getY).min().orElse(0); maxY = positions.stream().mapToInt(BlockPos::getY).max().orElse(0);
            int minZ = positions.stream().mapToInt(BlockPos::getZ).min().orElse(0), maxZ = positions.stream().mapToInt(BlockPos::getZ).max().orElse(0);
            center = new Vector3f((minX + maxX + 1) / 2f, (minY + maxY + 1) / 2f, (minZ + maxZ + 1) / 2f);
            fit = 85f / Math.max(maxY - minY + 1, Math.max(maxX - minX + 1, maxZ - minZ + 1) * 1.42f);
        }
        private Component layerLabel() {
            return text("layer", layer < 0 ? text("all") : Component.literal(Integer.toString(layer - minY + 1)));
        }
        private void nextLayer() { layer = layer < 0 ? minY : layer >= maxY ? -1 : layer + 1; }
        private void reset() { yaw = StructureRenderer.frontYaw(page.facing); pitch = 25; zoom = 1; layer = -1; }
        private Quaternionf rotation() { return new Quaternionf().rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw)); }
        private List<StructureRenderState.Cell> mesh() {
            return meshes.computeIfAbsent(layer, current -> {
                var level = Minecraft.getInstance().level;
                if (level == null) return List.of();
                var world = page.world(level.registryAccess());
                world.renderLayer = current;
                var cells = new ArrayList<StructureRenderState.Cell>();
                for (var entry : world.tiles.entrySet()) {
                    var pos = entry.getKey();
                    if (current >= 0 && pos.getY() != current || !(entry.getValue() instanceof IRenderedBlockObject rendered)) continue;
                    var builder = new GT6QuadBuilder();
                    GT6BlockModel.buildRendererQuads(builder, rendered, world.getBlockState(pos).getBlock(), world, pos.getX(), pos.getY(), pos.getZ());
                    cells.add(new StructureRenderState.Cell(pos, List.copyOf(builder.quads())));
                }
                return List.copyOf(cells);
            });
        }
        @Override public ScreenPosition getPosition() { return new ScreenPosition(0, 0); }
        @Override public ScreenRectangle getArea() { return new ScreenRectangle(0, 18, 200, 122); }
        @Override public void drawWidget(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            graphics.text(font, text("help"), 2, 3, 0xFF555555, false);
            graphics.fill(0, 18, 200, 140, 0xFF20242A);
            var pose = new Matrix3x2f(graphics.pose());
            var bounds = new ScreenRectangle(0, 18, 200, 122).transformMaxBounds(pose);
            ((GuiGraphicsAccessor) graphics).gtqualityneo$renderState().addPicturesInPictureState(new StructureRenderState(
                mesh(), new Vector3f(center), rotation(), 0, 18, 200, 140, fit * zoom, pose, bounds, bounds));
        }
        private BlockPos hit(double x, double y) {
            if (x < 0 || x >= 200 || y < 18 || y >= 140) return null;
            var inverse = rotation().conjugate();
            float vx = (float) (x - 100) / (fit * zoom), vy = (float) (79 - y) / (fit * zoom);
            var a = inverse.transform(new Vector3f(vx, vy, 1000)).add(center);
            var b = inverse.transform(new Vector3f(vx, vy, -1000)).add(center);
            var start = new Vec3(a.x, a.y, a.z); var end = new Vec3(b.x, b.y, b.z);
            double closest = Double.MAX_VALUE;
            BlockPos found = null;
            for (var cell : mesh()) {
                var p = cell.pos();
                var collision = new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1).clip(start, end);
                if (collision.isPresent() && collision.get().distanceToSqr(start) < closest) {
                    closest = collision.get().distanceToSqr(start); found = p;
                }
            }
            return found;
        }
        @Override public void getTooltip(ITooltipBuilder tooltip, double x, double y) {
            var pos = hit(x, y);
            if (pos != null) tooltip.add(page.stackAt(pos).getHoverName());
        }
        @Override public boolean handleInput(double x, double y, IJeiUserInput input) {
            if (input.getKey().getType() != InputConstants.Type.MOUSE || input.getKey().getValue() > 1) return false;
            // Input coordinates are relative to getArea(), whereas drawing coordinates are relative to the widget.
            y += 18;
            var pos = hit(x, y);
            if (pos == null) return true;
            if (!input.isSimulate() && runtime.get() != null) {
                var role = input.getKey().getValue() == 1 ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
                runtime.get().getRecipesGui().show(List.of(helpers.getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, page.stackAt(pos))));
            }
            return true;
        }
        @Override public boolean handleMouseScrolled(double x, double y, double dx, double dy) {
            if (dy == 0) return false;
            if (Minecraft.getInstance().hasShiftDown()) layer = layer < 0 ? minY : Math.floorMod(layer - minY + (dy > 0 ? 1 : -1), maxY - minY + 1) + minY;
            else zoom = Math.clamp(zoom * (dy > 0 ? 1.15f : 1 / 1.15f), .35f, 4);
            return true;
        }
        @Override public boolean handleMouseDragged(double x, double y, InputConstants.Key key, double dx, double dy) {
            if (key.getValue() != 0 || y >= 122) return false;
            yaw += (float) dx; pitch = Math.clamp(pitch + (float) dy, -85, 85);
            return true;
        }
    }
}
