package com.plainston.gtqualityneo.client;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Clip;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.TaffyPosition;
import gregapi.data.LH;
import gregapi.gui.ContainerCommonBasicMachine;
import gregapi.gui.Slot_Render;
import gregapi.tileentity.machines.MultiTileEntityBasicMachine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.Locale;

final class MachineScreen extends AbstractContainerScreen<ContainerCommonBasicMachine> {
    private final ModularUI ui;
    private final MachineLayout layout;
    private final String recipeCategory;
    private final String guiTexture;
    private final int progressDirection, progressCycles;

    MachineScreen(ContainerCommonBasicMachine menu) {
        this(menu, (MultiTileEntityBasicMachine) menu.mTileEntity);
    }

    private MachineScreen(ContainerCommonBasicMachine menu, MultiTileEntityBasicMachine machine) {
        this(menu, machine, new MachineLayout(machine.mRecipes.mInputItemsCount, machine.mRecipes.mOutputItemsCount,
            machine.mRecipes.mInputFluidCount, machine.mRecipes.mOutputFluidCount,
            menu.slots.stream().anyMatch(slot -> slot.container == machine
                && slot.getSlotIndex() == machine.mRecipes.mInputItemsCount + machine.mRecipes.mOutputItemsCount)));
    }

    private MachineScreen(ContainerCommonBasicMachine menu, MultiTileEntityBasicMachine machine, MachineLayout layout) {
        super(menu, menu.mInventoryPlayer, Component.literal(LH.get(machine.getInventoryNameGUI())),
            MachineLayout.WIDTH, layout.height());
        this.layout = layout;
        recipeCategory = machine.mRecipes.mNameNEI;
        guiTexture = machine.mGUITexture.toLowerCase(Locale.ROOT);
        progressDirection = machine.mRecipes.mProgressBarDirection;
        progressCycles = Byte.toUnsignedInt(machine.mRecipes.mProgressBarAmount);
        var root = InventoryUi.panel(menu, title, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            boolean player = slot.container == menu.mInventoryPlayer;
            int index = slot.getSlotIndex();
            var position = player ? new MachineLayout.Position(layout.playerX(index), layout.playerY(index))
                : layout.position(index);
            // Read the old GUI coordinates before LDLib repositions the original Slot.
            int textureX = slot.x - 1, textureY = slot.y - 1;
            var element = InventoryUi.slot(menu, slot, position.x(), position.y());
            if (slot instanceof Slot_Render) {
                boolean input = index - layout.fluidStart() < layout.inputFluids();
                element.style(s -> s.background(SpriteTexture.of(guiTexture).setSprite(textureX, textureY, 18, 18))
                    .tooltips(text(input ? "fluid_input" : "fluid_output")));
            }
            root.addChild(element);
        }
        root.addChild(progressArrow());
        ui = ModularUI.of(UI.of(root), menu.mInventoryPlayer.player);
        ((IModularUIHolderMenu) menu).setModularUI(ui);
    }

    private static Component text(String key) {
        return Component.translatable("gtqualityneo.machine." + key);
    }

    private UIElement progressArrow() {
        var arrow = new UIElement()
            .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(MachineLayout.ARROW_X).top(layout.arrowY())
                .width(MachineLayout.ARROW_WIDTH).height(MachineLayout.ARROW_HEIGHT))
            .style(s -> s.background(SpriteTexture.of(guiTexture).setSprite(78, 24, 20, 18))
                .tooltips(text("recipes")));
        var fill = new UIElement().layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(0)
            .width(0).height(0)).style(s -> s.clip(Clip.SCISSOR));
        var overlay = new UIElement()
            .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).width(20).height(18))
            .style(s -> s.background(SpriteTexture.of(guiTexture).setSprite(176, 0, 20, 18)));
        fill.addChild(overlay);
        arrow.addChild(fill);
        Runnable update = () -> {
            var frame = MachineProgress.frame(menu.mProgressBar, progressDirection, progressCycles);
            fill.layout(l -> l.left(frame.x()).top(frame.y()).width(frame.width()).height(frame.height()));
            overlay.layout(l -> l.left(-frame.x()).top(-frame.y()));
        };
        update.run();
        arrow.addEventListener(UIEvents.TICK, event -> update.run());
        return arrow;
    }

    @Override protected void init() {
        super.init();
        setFocused(ModularUIClientAccess.getWidget(ui));
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x() - leftPos, y = event.y() - topPos;
        if (x >= MachineLayout.ARROW_X && x < MachineLayout.ARROW_X + MachineLayout.ARROW_WIDTH
            && y >= layout.arrowY() && y < layout.arrowY() + MachineLayout.ARROW_HEIGHT
            && gregapi.data.CS.NEI && gregapi.jei.GT6_JEI_Plugin.showRecipeCategory(recipeCategory)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}
}
