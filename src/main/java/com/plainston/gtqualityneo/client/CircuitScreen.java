package com.plainston.gtqualityneo.client;

import com.plainston.gtqualityneo.qol.QolNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class CircuitScreen extends Screen {
    private final QolNetwork.OpenCircuit state;
    public CircuitScreen(QolNetwork.OpenCircuit state) {
        super(Component.translatable("gtqualityneo.circuit.title"));
        this.state = state;
    }
    @Override protected void init() {
        int left = width / 2 - 100, top = height / 2 - 60;
        for (int number = 0; number <= 24; number++) {
            final int selected = number;
            var button = Button.builder(Component.literal(Integer.toString(number)), ignored -> {
                ClientPacketDistributor.sendToServer(new QolNetwork.SelectCircuit(state.slot(), state.original(), selected));
                onClose();
            }).bounds(left + number % 5 * 40, top + number / 5 * 24, 36, 20).build();
            if (number == (state.original() & 255)) button.setMessage(Component.literal("[" + number + "]"));
            addRenderableWidget(button);
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.centeredText(font, title, width / 2, height / 2 - 80, -1);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
