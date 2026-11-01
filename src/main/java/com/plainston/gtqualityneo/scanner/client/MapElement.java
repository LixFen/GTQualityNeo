package com.plainston.gtqualityneo.scanner.client;

import com.plainston.gtqualityneo.scanner.*;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.Identifier;
import java.util.*;
import java.util.function.Consumer;

/** One cached GPU texture per map; unchanged frames submit one quad and only the live markers. */
public final class MapElement extends UIElement {
    private final Consumer<ClientState.BlockMark> select;
    private final Map<Integer, ColorRectTexture> colours = new HashMap<>();
    private float zoom = 1, panX, panY, lastX, lastY, startX, startY;
    private boolean dragging;
    private int stamp = -1, side;
    private Set<String> filter = Set.of();
    private ScanGrid grid;
    private boolean terrain;
    private final ScanMapRaster raster = new ScanMapRaster();
    private final Identifier textureId = Identifier.fromNamespaceAndPath(ScannerIntegration.ID, "scan_map/" + UUID.randomUUID());
    private DynamicTexture texture;
    public MapElement(Consumer<ClientState.BlockMark> select) {
        this.select = select;
        layout(l -> l.paddingAll(3));
        style(s -> s.background(MCSprites.RECT_BLACK));
        addEventListener(UIEvents.MOUSE_WHEEL, e -> {
            float old = zoom;
            zoom = Math.max(1, Math.min(8, zoom * (e.deltaY > 0 ? 1.25f : .8f)));
            var p = getLocalMouse(e.x, e.y);
            float cx = p.x - getContentX() - getContentWidth() / 2;
            float cy = p.y - getContentY() - getContentHeight() / 2;
            panX = cx - (cx - panX) * zoom / old;
            panY = cy - (cy - panY) * zoom / old;
            e.stopPropagation();
        });
        addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button != 0) return;
            dragging = true; startX = lastX = e.x; startY = lastY = e.y;
        });
        addEventListener(UIEvents.MOUSE_MOVE, e -> {
            if (dragging) { panX += e.x - lastX; panY += e.y - lastY; lastX = e.x; lastY = e.y; }
        });
        addEventListener(UIEvents.MOUSE_UP, e -> {
            if (e.button != 0) return;
            boolean click = dragging && Math.abs(e.x - startX) + Math.abs(e.y - startY) < 3;
            dragging = false;
            if (click) pick(e.x, e.y);
        });
        addEventListener(UIEvents.MOUSE_LEAVE, e -> dragging = false);
    }
    public void reset() { zoom = 1; panX = panY = 0; }
    private float scale() { return Math.min(getContentWidth(), getContentHeight()) / Math.max(1, side) * zoom; }
    private float left() { return getContentX() + (getContentWidth() - side * scale()) / 2 + panX; }
    private float top() { return getContentY() + (getContentHeight() - side * scale()) / 2 + panY; }
    private void pick(float mouseX, float mouseY) {
        rebuild();
        if (side == 0 || net.minecraft.client.Minecraft.getInstance().player == null || !net.minecraft.client.Minecraft.getInstance().player.level().dimension().equals(ClientState.dimension)) return;
        var p = getLocalMouse(mouseX, mouseY);
        int x = (int) Math.floor((p.x - left()) / scale()), z = (int) Math.floor((p.y - top()) / scale());
        if (x < 0 || z < 0 || x >= side || z >= side) return;
        if (raster.key(x, z) != null) select.accept(new ClientState.BlockMark(ClientState.grid.chunkX() * 16 + x, raster.height(x, z),
                ClientState.grid.chunkZ() * 16 + z, raster.key(x, z), net.minecraft.client.Minecraft.getInstance().player.level().dimension()));
    }
    private void rebuild() {
        if (ClientState.grid == null) { side = 0; closeTexture(); return; }
        if (texture != null && grid == ClientState.grid && stamp == ClientState.revision
                && filter.equals(ClientState.filter) && terrain == ClientState.terrain) return;
        grid = ClientState.grid; stamp = ClientState.revision;
        filter = Set.copyOf(ClientState.filter); terrain = ClientState.terrain;
        int changed = raster.update(grid, ClientState.chunks.values(), filter, terrain, ClientState::colour);
        side = raster.side();
        if (texture == null || texture.getPixels().getWidth() != side) {
            closeTexture();
            texture = new DynamicTexture("GT6 scanner map", side, side, false);
            Minecraft.getInstance().getTextureManager().register(textureId, texture);
            changed = side * side;
        }
        if (changed == 0) return;
        var image = texture.getPixels();
        int[] pixels = raster.pixels();
        if (raster.fullUpdate() || changed == side * side) {
            for (int z = 0; z < side; z++) for (int x = 0; x < side; x++) image.setPixel(x, z, pixels[z * side + x]);
            texture.upload();
        } else {
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            for (int i = 0; i < raster.dirtyCount(); i++) {
                int index = raster.dirtyChunk(i);
                int cx = index / grid.width() * 16, cz = index % grid.width() * 16;
                for (int z = cz; z < cz + 16; z++) for (int x = cx; x < cx + 16; x++)
                    image.setPixel(x, z, pixels[z * side + x]);
                encoder.writeToTexture(texture.getTexture(), image, 0, 0, cx, cz, 16, 16, cx, cz);
            }
        }
    }
    public void closeTexture() {
        if (texture == null) return;
        Minecraft.getInstance().getTextureManager().release(textureId);
        texture = null;
    }
    @Override protected void drawBackgroundAdditional(IGUIContext context) {
        rebuild();
        if (side == 0) return;
        float scale = scale(), left = left(), top = top();
        context.enableScissor(getContentX(), getContentY(), getContentWidth(), getContentHeight());
        ((GUIContext) context).blit(RenderPipelines.GUI_TEXTURED, textureId, left, top,
                side * scale, side * scale, 0, 0, 1, 1, -1);
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && player.level().dimension().equals(ClientState.dimension)) {
            float px = left + ((float) player.getX() - ClientState.grid.chunkX() * 16) * scale;
            float pz = top + ((float) player.getZ() - ClientState.grid.chunkZ() * 16) * scale;
            rect(context, 0xffffffff, px - 3, pz - 1, 7, 3);
            rect(context, 0xffffffff, px - 1, pz - 3, 3, 7);
        }
        var mark = ClientState.mark;
        if (mark != null && player != null && player.level().dimension().equals(mark.dimension())) {
            float mx = left + (mark.x() - ClientState.grid.chunkX() * 16 + .5f) * scale;
            float mz = top + (mark.z() - ClientState.grid.chunkZ() * 16 + .5f) * scale;
            rect(context, 0xffffd56a, mx - 3, mz - 3, 7, 1);
            rect(context, 0xffffd56a, mx - 3, mz + 3, 7, 1);
            rect(context, 0xffffd56a, mx - 3, mz - 3, 1, 7);
            rect(context, 0xffffd56a, mx + 3, mz - 3, 1, 7);
        }
        context.disableScissor();
    }
    private void rect(IGUIContext c, int colour, float x, float y, float width, float height) {
        c.drawTexture(colours.computeIfAbsent(colour, ColorRectTexture::new), x, y, width, height);
    }
}
