package com.plainston.gtqualityneo.client.screen;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lowdragmc.lowdraglib2.gui.texture.FluidStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.plainston.gtqualityneo.fluid.CreativeTankMenu;
import com.plainston.gtqualityneo.qol.QolNetwork;
import dev.vfyjxf.taffy.style.TaffyPosition;
import gregapi.data.FL;
import gregapi.util.ST;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    private static final SpriteTexture SLOT = SpriteTexture.of("minecraft:textures/gui/container/generic_54.png")
        .setSprite(7, 17, 18, 18).setBorder(1);
    private final ModularUI ui;
    private final TextField rate = new TextField();
    private final Button automatic = new Button();
    private final Label fluidName = new Label();
    private final FluidStackTexture fluidTexture = new FluidStackTexture(FluidStack.EMPTY);

    public CreativeTankScreen(CreativeTankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
        var root = new UIElement().layout(l -> l.width(imageWidth).height(imageHeight))
            .style(s -> s.background(MCSprites.RECT));
        root.addChild(label(title, 8, 6));
        root.addChild(label(inventory.getDisplayName(), 8, 91));

        var fluidSlot = new UIElement().layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
            .left(7).top(19).width(18).height(18)).style(s -> s.background(SLOT).tooltips(text("fluid_tip")));
        fluidSlot.addChild(new UIElement().layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
            .left(1).top(1).width(16).height(16)).style(s -> s.background(fluidTexture)));
        root.addChild(fluidSlot);
        fluidName.textStyle(s -> s.textColor(0xff404040).textShadow(false));
        root.addChild(fluidName.layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
            .left(29).top(25).width(112).height(9)));

        root.addChild(automatic.setOnClick(event -> send(2, 0, FluidStack.EMPTY))
            .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(8).top(42).width(160).height(20)));
        rate.setTextRegexValidator("[0-9]{0,10}").setCharValidator(character -> character >= '0' && character <= '9');
        rate.style(s -> s.tooltips(text("rate")));
        root.addChild(rate.layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
            .left(8).top(67).width(88).height(16)));
        root.addChild(new Button().setText(text("apply")).setOnClick(event -> applyRate())
            .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(102).top(65).width(66).height(20)));
        root.addChild(new Button().setText(Component.literal("X")).setOnClick(event -> send(3, 0, FluidStack.EMPTY))
            .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(147).top(18).width(21).height(20)));

        var holder = (IModularUIHolderMenu) (Object) menu;
        for (var slot : menu.slots) {
            var element = new ItemSlot().bind(slot).slotStyle(s -> s.isPlayerSlot(true));
            element.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(slot.x - 1).top(slot.y - 1)
                .width(18).height(18).paddingAll(1));
            element.style(s -> s.background(SLOT));
            holder.ldlib2$addSlot(element);
            root.addChild(element);
        }
        ui = ModularUI.of(UI.of(root, StylesheetManager.MC), inventory.player);
        holder.setModularUI(ui);
        updateSettings();
    }

    private static Component text(String key) { return Component.translatable("gtqualityneo.creative_tank." + key); }

    private static UIElement label(Component text, int x, int y) {
        return new Label().setText(text).textStyle(s -> s.textColor(0xff404040).textShadow(false))
            .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y));
    }

    @Override protected void init() {
        super.init();
        setFocused(ModularUIClientAccess.getWidget(ui));
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

    private void applyRate() {
        if (rate.isError()) {
            rate.setText(Integer.toString(menu.settings.rate()));
            return;
        }
        try {
            send(1, Integer.parseInt(rate.getRawText()), FluidStack.EMPTY);
            rate.blur();
        } catch (NumberFormatException ignored) {
            rate.setText(Integer.toString(menu.settings.rate()));
        }
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (rate.isFocused() && (event.key() == 257 || event.key() == 335)) {
            applyRate();
            return true;
        }
        if (rate.isFocused() && event.isEscape()) { rate.blur(); return true; }
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
        updateSettings();
    }

    private void updateSettings() {
        if (!rate.isFocused()) rate.setText(Integer.toString(menu.settings.rate()));
        automatic.setText(Component.translatable("gtqualityneo.creative_tank.automatic_state",
            text(menu.settings.automatic() ? "on" : "off")));
        var fluid = menu.settings.fluid();
        fluidTexture.setFluids(fluid);
        Component name = fluid.isEmpty() ? text("empty") : fluid.getFluidType().getDescription(fluid);
        fluidName.setText(Component.literal(Minecraft.getInstance().font.plainSubstrByWidth(name.getString(), 112)));
        fluidName.style(s -> s.tooltips(name));
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}
}
