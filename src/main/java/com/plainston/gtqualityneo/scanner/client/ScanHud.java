package com.plainston.gtqualityneo.scanner.client;

import com.plainston.gtqualityneo.scanner.ScanNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.*;

final class ScanHud {
    static void draw(GuiGraphicsExtractor graphics) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (!ClientState.hud || player == null || mc.options.hideGui || mc.screen != null) return;
        int x = Math.min(HudConfig.X.get(), Math.max(0, mc.getWindow().getGuiScaledWidth() - 12));
        int y = Math.min(HudConfig.Y.get(), Math.max(0, mc.getWindow().getGuiScaledHeight() - 70));
        boolean sameDimension = ClientState.grid != null && player.level().dimension().equals(ClientState.dimension);
        if (sameDimension && HudConfig.LIST.get()) {
            int cx = player.chunkPosition().x() - ClientState.grid.chunkX(), cz = player.chunkPosition().z() - ClientState.grid.chunkZ();
            if (cx >= 0 && cz >= 0 && cx < ClientState.grid.width() && cz < ClientState.grid.width()) {
                var chunk = ClientState.chunks.get(cx * ClientState.grid.width() + cz);
                if (chunk != null && chunk.available()) {
                    var entries = chunk.palette().stream().filter(r -> ClientState.filter.isEmpty() || ClientState.filter.contains(r.key()))
                            .sorted(Comparator.comparingInt(ScanNetwork.Resource::count).reversed()).limit(5).toList();
                    for (var r : entries) {
                        String line = ClientState.name(r.key()).getString() + "  " + r.count();
                        graphics.fill(x - 2, y - 1, x + mc.font.width(line) + 3, y + 10, 0xd9101b29);
                        graphics.text(mc.font, Component.literal(line), x, y, ClientState.colour(r), false); y += 11;
                    }
                }
            }
        }
        var mark = ClientState.mark;
        if (mark != null && player.level().dimension().equals(mark.dimension())) {
            double distance = Math.sqrt(player.distanceToSqr(mark.x() + .5, mark.y() + .5, mark.z() + .5));
            String line = ClientState.name(mark.resource()).getString() + "  " + mark.x() + ", " + mark.y() + ", " + mark.z() + "  (" + (int) distance + "m)";
            graphics.fill(x - 2, y - 1, Math.min(mc.getWindow().getGuiScaledWidth(), x + mc.font.width(line) + 3), y + 10, 0xd9101b29);
            graphics.text(mc.font, Component.literal(line), x, y, 0xff72e5c0, false);
        }
    }
    private ScanHud() {}
}
