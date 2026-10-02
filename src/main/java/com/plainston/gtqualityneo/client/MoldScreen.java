package com.plainston.gtqualityneo.client;

import com.plainston.gtqualityneo.qol.MoldInteraction;
import com.plainston.gtqualityneo.qol.QolNetwork;
import gregapi.data.LH;
import gregapi.oredict.OreDictPrefix;
import gregtech.tileentity.tools.MultiTileEntityMold;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public final class MoldScreen extends Screen {
    private final QolNetwork.OpenMold state;
    private final List<Map.Entry<Integer, OreDictPrefix>> choices;
    private int page;

    public MoldScreen(QolNetwork.OpenMold state) {
        super(Component.translatable("gtqualityneo.mold.title"));
        this.state = state;
        choices = new ArrayList<>(MoldInteraction.choices().entrySet());
        choices.sort((left, right) -> {
            if (left.getKey().equals(right.getKey())) return 0;
            if (left.getKey() == state.lastShape()) return -1;
            if (right.getKey() == state.lastShape()) return 1;
            boolean leftRaw = left.getValue().mNameLocal.toLowerCase(Locale.ROOT).startsWith("raw ");
            boolean rightRaw = right.getValue().mNameLocal.toLowerCase(Locale.ROOT).startsWith("raw ");
            if (leftRaw != rightRaw) return leftRaw ? 1 : -1;
            int order = name(left.getValue()).compareToIgnoreCase(name(right.getValue()));
            return order != 0 ? order : left.getKey().compareTo(right.getKey());
        });
    }

    private static String name(OreDictPrefix prefix) { return LH.get("oredict.prefix." + prefix.mNameInternal, prefix.mNameLocal); }
    @Override protected void init() {
        int left = width / 2 - 100;
        for (int i = 0; i < 8 && page * 8 + i < choices.size(); i++) {
            var choice = choices.get(page * 8 + i);
            addRenderableWidget(Button.builder(Component.literal(name(choice.getValue())), button -> select(choice.getKey()))
                .bounds(left, height / 2 - 82 + i * 21, 200, 20).build());
        }
        var previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> { page--; rebuildWidgets(); })
            .bounds(left, height / 2 + 91, 65, 20).build());
        previous.active = page > 0;
        addRenderableWidget(Button.builder(Component.translatable("gtqualityneo.mold.clear"), button -> select(0))
            .bounds(left + 68, height / 2 + 91, 64, 20).build());
        var next = addRenderableWidget(Button.builder(Component.literal(">"), button -> { page++; rebuildWidgets(); })
            .bounds(left + 135, height / 2 + 91, 65, 20).build());
        next.active = (page + 1) * 8 < choices.size();
    }
    private void select(int shape) {
        ClientPacketDistributor.sendToServer(new QolNetwork.SelectMold(state.pos(), shape));
        onClose();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.centeredText(font, title, width / 2, height / 2 - 114, -1);
        var prefix = MultiTileEntityMold.MOLD_RECIPES.get(state.shape());
        Component current = state.shape() == 0 ? Component.translatable("gtqualityneo.mold.empty")
            : prefix == null ? Component.translatable("gtqualityneo.mold.custom") : Component.literal(name(prefix));
        graphics.centeredText(font, Component.translatable("gtqualityneo.mold.current", current), width / 2, height / 2 - 101, 0xffaaaaaa);
        graphics.centeredText(font, (page + 1) + "/" + Math.max(1, (choices.size() + 7) / 8), width / 2, height / 2 + 82, 0xffaaaaaa);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
