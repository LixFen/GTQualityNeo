package com.plainston.gtqualityneo.client;

import com.plainston.gtqualityneo.fluid.CreativeTankMenu;
import com.plainston.gtqualityneo.qol.QolNetwork;
import gregapi.data.FL;
import gregapi.util.ST;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class CreativeTankScreen extends AbstractContainerScreen<CreativeTankMenu> {
    private EditBox rate;
    private Button automatic;
    public CreativeTankScreen(CreativeTankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
        inventoryLabelY = 91;
    }
    private static Component text(String key) { return Component.translatable("gtqualityneo.creative_tank." + key); }
    @Override protected void init() {
        super.init();
        rate = addRenderableWidget(new EditBox(font, leftPos + 8, topPos + 67, 88, 16, text("rate")));
        rate.setMaxLength(10);
        rate.setFilter(value -> value.matches("[0-9]*"));
        rate.setValue(Integer.toString(menu.settings.rate()));
        automatic = addRenderableWidget(Button.builder(text("automatic"), button -> send(2, 0, FluidStack.EMPTY))
            .bounds(leftPos + 8, topPos + 42, 160, 20).build());
        addRenderableWidget(Button.builder(text("apply"), button -> {
            try { send(1, Integer.parseInt(rate.getValue()), FluidStack.EMPTY); rate.setFocused(false); }
            catch (NumberFormatException ignored) { rate.setValue(Integer.toString(menu.settings.rate())); }
        }).bounds(leftPos + 102, topPos + 65, 66, 20).build());
        addRenderableWidget(Button.builder(Component.literal("X"), button -> send(3, 0, FluidStack.EMPTY))
            .bounds(leftPos + 147, topPos + 18, 21, 20).build());
    }
    public boolean overFluid(double x, double y) {
        return x >= leftPos + 7 && x < leftPos + 25 && y >= topPos + 19 && y < topPos + 37;
    }
    public void selectFluid(FluidStack fluid) { if (!fluid.isEmpty()) send(0, 0, fluid.copyWithAmount(1)); }
    public boolean select(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof gregapi.item.ItemFluidDisplay) {
            var displayed = FL.fluid(ST.meta_(stack));
            if (displayed == null) return false;
            selectFluid(new FluidStack(displayed, 1));
            return true;
        }
        FluidStack fluid = FluidUtil.getFirstStackContained(stack.copy());
        if (fluid.isEmpty()) {
            fluid = FL.getFluid(stack.copy(), true);
            if (fluid == null || fluid.isEmpty()) return false;
        }
        selectFluid(fluid);
        return true;
    }
    private void send(int action, int value, FluidStack fluid) {
        ClientPacketDistributor.sendToServer(new QolNetwork.ConfigureTank(menu.containerId, action, value, fluid));
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (rate.isFocused() && (event.key() == 257 || event.key() == 335)) {
            try { send(1, Integer.parseInt(rate.getValue()), FluidStack.EMPTY); rate.setFocused(false); }
            catch (NumberFormatException ignored) { rate.setValue(Integer.toString(menu.settings.rate())); }
            return true;
        }
        if (rate.isFocused() && event.isEscape()) { rate.setFocused(false); return true; }
        return super.keyPressed(event);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (overFluid(event.x(), event.y()) && (event.button() == 0 || event.button() == 1)) {
            if (event.button() == 1 && menu.getCarried().isEmpty()) send(3, 0, FluidStack.EMPTY);
            else select(menu.getCarried());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public void containerTick() {
        super.containerTick();
        if (!rate.isFocused()) rate.setValue(Integer.toString(menu.settings.rate()));
        automatic.setMessage(Component.translatable("gtqualityneo.creative_tank.automatic_state",
            text(menu.settings.automatic() ? "on" : "off")));
    }
    @Override public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
        graphics.fill(leftPos + 7, topPos + 19, leftPos + 25, topPos + 37, 0xff555555);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff555555);
            graphics.fill(x, y, x + 16, y + 16, 0xff8b8b8b);
        }
        var fluid = menu.settings.fluid();
        if (!fluid.isEmpty()) graphics.item(ST.nn(FL.display(fluid, false, false)), leftPos + 8, topPos + 20);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        var fluid = menu.settings.fluid();
        String name = fluid.isEmpty() ? text("empty").getString() : fluid.getFluidType().getDescription(fluid).getString();
        graphics.text(font, font.plainSubstrByWidth(name, 112), 29, 25, 0xff404040, false);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (overFluid(mouseX, mouseY)) graphics.setComponentTooltipForNextFrame(font,
            java.util.List.of(text("fluid_tip")), mouseX, mouseY);
    }
}
