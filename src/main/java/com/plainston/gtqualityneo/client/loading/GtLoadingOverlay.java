package com.plainston.gtqualityneo.client.loading;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.mixin.client.LoadingFrameInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class GtLoadingOverlay {
    private static boolean refreshing;
    private static long lastFrame;

    private GtLoadingOverlay() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener(GtLoadingOverlay::draw);
        GtLoadingProgress.onChange(GtLoadingOverlay::refresh);
    }
    private static Component text(String value) {
        if (value.startsWith("Config File: "))
            return Component.literal(value.substring(Math.max(value.lastIndexOf('\\'), value.lastIndexOf('/')) + 1));
        return Component.translatableWithFallback("gtqualityneo.loading." + value,
            value.replace("Loader_", "").replace('_', ' '));
    }
    private static void draw(ScreenEvent.Render.Post event) {
        if (!GTQualityNeo.CLIENT_CONFIG.isLoaded() || !GTQualityNeo.WORLD_LOADING_PROGRESS.get()) return;
        var state = GtLoadingProgress.snapshot();
        if (state == null) return;
        var graphics = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        int width = Math.min(260, event.getScreen().width - 16);
        if (width < 48) return;
        graphics.fill(6, 6, width + 10, 45, 0xD020242A);
        graphics.text(font, Component.translatable("gtqualityneo.loading.title"), 12, 11, 0xFFF0F0F0);
        String detail = text(state.stage()).getString();
        if (!state.detail().isEmpty()) detail += " · " + text(state.detail()).getString();
        graphics.text(font, font.plainSubstrByWidth(detail, width - 12), 12, 23, 0xFFBFCAD5);
        int left = 12, right = width + 4, top = 37, span = right - left;
        graphics.fill(left, top, right, top + 3, 0xFF404852);
        if (state.total() > 0) {
            int filled = (int) ((long) span * Math.min(state.current(), state.total()) / state.total());
            graphics.fill(left, top, left + filled, top + 3, 0xFF68C8B7);
        }
        // A moving highlight keeps long individual tasks visibly alive, without an estimated duration.
        int highlight = (int) (Util.getMillis() / 12 % (span + 32)) - 32;
        graphics.fill(left + Math.max(0, highlight), top, left + Math.min(span, highlight + 32), top + 3, 0xFFB1E8DC);
    }
    private static void refresh() {
        if (!GTQualityNeo.CLIENT_CONFIG.isLoaded() || !GTQualityNeo.WORLD_LOADING_PROGRESS.get()) return;
        var minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread() || minecraft.screen == null || minecraft.getOverlay() != null || refreshing) return;
        long now = Util.getMillis();
        // Client-thread recipe loading cannot return to the normal frame loop between every stage.
        if (now - lastFrame < 50) return;
        lastFrame = now;
        refreshing = true;
        try { ((LoadingFrameInvoker) minecraft).gtqualityneo$loadingFrame(false); }
        finally { refreshing = false; }
    }
}
