package com.plainston.gtqualityneo.integration.multiblock;

import com.plainston.gtqualityneo.mixin.client.GuiGraphicsAccessor;
import gregapi.render.GT6BlockModel;
import gregapi.render.GT6QuadBuilder;
import gregapi.render.IRenderedBlockObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared scene state and rendering for both recipe browsers. */
final class StructureScene {
    private final MultiblockCatalog.Page page;
    private final Map<Integer, List<StructureRenderState.Cell>> meshes = new HashMap<>();
    private final Vector3f center;
    private final int minY, maxY;
    private final float fit;
    private float yaw, pitch = 25, zoom = 1;
    private int layer = -1;
    StructureScene(MultiblockCatalog.Page page) {
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
    Component layerLabel() {
        return text("layer", layer < 0 ? text("all") : Component.literal(Integer.toString(layer - minY + 1)));
    }
    void nextLayer() { layer = layer < 0 ? minY : layer >= maxY ? -1 : layer + 1; }
    void reset() { yaw = StructureRenderer.frontYaw(page.facing); pitch = 25; zoom = 1; layer = -1; }
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
    void draw(GuiGraphicsExtractor graphics) {
        var font = Minecraft.getInstance().font;
        graphics.text(font, text("help"), 2, 3, 0xFF555555, false);
        graphics.fill(0, 18, 200, 140, 0xFF20242A);
        var pose = new Matrix3x2f(graphics.pose());
        var bounds = new ScreenRectangle(0, 18, 200, 122).transformMaxBounds(pose);
        ((GuiGraphicsAccessor) graphics).gtqualityneo$renderState().addPicturesInPictureState(new StructureRenderState(
            mesh(), new Vector3f(center), rotation(), 0, 18, 200, 140, fit * zoom, pose, bounds, bounds));
    }
    BlockPos hit(double x, double y) {
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

    static Component text(String key, Object... args) {
        return Component.translatable("gtqualityneo.jei.multiblock." + key, args);
    }

    void zoom(boolean in) {
        zoom = Math.clamp(zoom * (in ? 1.15f : 1 / 1.15f), .35f, 4);
    }

    void scroll(double delta, boolean layers) {
        if (layers) layer = layer < 0 ? minY : Math.floorMod(layer - minY + (delta > 0 ? 1 : -1), maxY - minY + 1) + minY;
        else zoom(delta > 0);
    }

    void rotate(double dx, double dy) {
        yaw += (float) dx;
        pitch = Math.clamp(pitch + (float) dy, -85, 85);
    }
}
