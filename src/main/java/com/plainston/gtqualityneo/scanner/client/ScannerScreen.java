package com.plainston.gtqualityneo.scanner.client;

import com.plainston.gtqualityneo.scanner.*;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import java.util.*;

public final class ScannerScreen {
    private final Label status = panelLabel();
    private final Label selection = panelLabel();
    private final ScrollerView results = new ScrollerView();
    private String search = "";
    private int revision = -1;
    private int listTicks;
    private int listRevision = -1;
    private Set<String> listed = Set.of();
    private final Map<String, Button> rows = new HashMap<>();
    private final MapElement map = new MapElement(this::selected);
    private ScannerScreen() {}
    public static ModularUIScreen create() { return new ScannerScreen().build(); }
    private ModularUIScreen build() {
        var mc = Minecraft.getInstance();
        float width = Math.min(760, mc.getWindow().getGuiScaledWidth() - 12);
        float height = Math.min(450, mc.getWindow().getGuiScaledHeight() - 12);
        var root = new UIElement().layout(l -> l.width(width).height(height).paddingAll(8).gapAll(5))
                .style(s -> s.background(MCSprites.RECT));
        root.addChild(panelLabel().setText(Component.translatable("gtqualityneo.scanner.title")));
        boolean narrow = width < 560;
        var modes = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).wrap(FlexWrap.WRAP).gapAll(3).height(narrow ? 45 : 22).flexShrink(0));
        for (var mode : ScanMode.values()) {
            modes.addChild(button(Component.translatable(mode.translationKey()), () -> {
                if (ClientState.scanning) return;
                ClientState.mode = mode; startScan();
            }).layout(l -> l.widthPercent(narrow ? 24 : 11.7f).height(21).flexShrink(0)));
        }
        root.addChild(modes);
        root.addChild(status);
        var body = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(7).flex(1));
        body.addChild(map.layout(l -> l.flex(1).heightPercent(100)));
        var sidebar = new UIElement().layout(l -> l.width(Math.min(190, width * .34f)).gapAll(4));
        sidebar.addChild(new TextField().setText("").textFieldStyle(s -> s.placeholder(Component.translatable("gtqualityneo.scanner.search")))
                .setTextResponder(text -> { search = text.toLowerCase(Locale.ROOT); rebuildResults(); }));
        sidebar.addChild(button(Component.translatable("gtqualityneo.scanner.all"), () -> { ClientState.filter.clear(); rebuildResults(); }));
        sidebar.addChild(results.layout(l -> l.flex(1).widthPercent(100)));
        body.addChild(sidebar); root.addChild(body);
        root.addChild(selection.setText(Component.translatable("gtqualityneo.scanner.map_help")));
        var footer = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(5).height(22));
        footer.addChildren(
                button(Component.translatable("gtqualityneo.scanner.rescan"), this::startScan),
                button(Component.translatable("gtqualityneo.scanner.cancel"), () -> ClientPacketDistributor.sendToServer(new ScanNetwork.Request(0, false, true))),
                button(Component.translatable("gtqualityneo.scanner.terrain"), () -> ClientState.terrain = !ClientState.terrain),
                button(Component.translatable("gtqualityneo.scanner.hud"), () -> ClientState.hud = !ClientState.hud),
                button(Component.translatable("gtqualityneo.scanner.clear_target"), () -> ClientState.mark = null),
                button(Component.translatable("gtqualityneo.scanner.reset_view"), map::reset));
        root.addChild(footer);
        root.addEventListener(UIEvents.TICK, e -> {
            if (revision != ClientState.revision) {
                revision = ClientState.revision;
                status.setText(Component.translatable(ClientState.status).append(ClientState.grid == null ? "" : "  " + ClientState.chunks.size() + "/" + ClientState.grid.width() * ClientState.grid.width()));
            }
            if (listRevision != ClientState.revision &&
                    (!listed.equals(ClientState.counts.keySet()) || !ClientState.scanning || ++listTicks >= 10)) {
                updateResults(); listTicks = 0; listRevision = ClientState.revision;
            }
        });
        rebuildResults();
        return new ModularUIScreen(ModularUI.of(UI.of(root, StylesheetManager.MC)), Component.translatable("gtqualityneo.scanner.title")) {
            @Override public void removed() { map.closeTexture(); super.removed(); }
        };
    }
    private static Label panelLabel() {
        var label = new Label();
        label.textStyle(s -> s.textColor(0xff404040).textShadow(false));
        return label;
    }
    private static Button button(Component text, Runnable action) {
        return new Button().setText(text).setOnClick(e -> action.run());
    }
    private void startScan() {
        if (ClientState.scanning) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var hand = player.getMainHandItem().getItem() instanceof ScannerItem ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (!(player.getItemInHand(hand).getItem() instanceof ScannerItem)) return;
        ClientState.status = "gtqualityneo.scanner.scanning";
        ClientState.revision++;
        ClientPacketDistributor.sendToServer(new ScanNetwork.Request(ClientState.mode.ordinal(), hand == InteractionHand.OFF_HAND, false));
    }
    private void rebuildResults() {
        results.clearAllScrollViewChildren();
        rows.clear();
        rowLabels.clear();
        listed = Set.copyOf(ClientState.counts.keySet());
        ClientState.counts.entrySet().stream().filter(e -> ClientState.name(e.getKey()).getString().toLowerCase(Locale.ROOT).contains(search))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).forEach(e -> {
                    String label = (ClientState.filter.contains(e.getKey()) ? "✓ " : "") + ClientState.name(e.getKey()).getString() + "  " + e.getValue();
                    var row = button(Component.literal(label), () -> {
                        if (!ClientState.filter.add(e.getKey())) ClientState.filter.remove(e.getKey());
                        updateLabels();
                    });
                    rows.put(e.getKey(), row);
                    results.addScrollViewChild(row.layout(l -> l.widthPercent(100).height(21).flexShrink(0)));
                });
    }
    private void updateResults() {
        if (!listed.equals(ClientState.counts.keySet())) rebuildResults();
        else updateLabels();
    }
    private final Map<String, String> rowLabels = new HashMap<>();
    private void updateLabels() {
        rows.forEach((key, row) -> {
            String text = (ClientState.filter.contains(key) ? "✓ " : "") + ClientState.name(key).getString() + "  " + ClientState.counts.getOrDefault(key, 0);
            if (!text.equals(rowLabels.put(key, text))) row.setText(Component.literal(text));
        });
    }
    private void selected(ClientState.BlockMark mark) {
        ClientState.mark = mark;
        selection.setText(ClientState.name(mark.resource()).copy().append("  X " + mark.x() + "  Y " + mark.y() + "  Z " + mark.z()));
        Minecraft.getInstance().keyboardHandler.setClipboard(mark.x() + " " + mark.y() + " " + mark.z());
    }
}
