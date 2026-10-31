package com.plainston.gtqualityneo.client;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import gregapi.data.LH;
import gregapi.gui.ContainerCommon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

final class StorageScreen extends AbstractContainerScreen<ContainerCommon> {
    private final ModularUI ui;

    StorageScreen(ContainerCommon menu) {
        this(menu, new StorageLayout((int) menu.slots.stream()
            .filter(slot -> slot.container != menu.mInventoryPlayer).count()));
    }

    private StorageScreen(ContainerCommon menu, StorageLayout layout) {
        super(menu, menu.mInventoryPlayer, Component.literal(LH.get(menu.mTileEntity.getInventoryNameGUI())),
            StorageLayout.WIDTH, layout.height());
        var root = InventoryUi.panel(menu, title, imageWidth, imageHeight);
        var holder = (IModularUIHolderMenu) menu;
        int storageIndex = 0;
        for (var slot : menu.slots) {
            boolean playerSlot = slot.container == menu.mInventoryPlayer;
            int index = playerSlot ? slot.getSlotIndex() : storageIndex++;
            int x = playerSlot ? layout.playerX(index) : layout.storageX(index);
            int y = playerSlot ? layout.playerY(index) : layout.storageY(index);
            root.addChild(InventoryUi.slot(menu, slot, x, y));
        }
        ui = ModularUI.of(UI.of(root), menu.mInventoryPlayer.player);
        holder.setModularUI(ui);
    }

    @Override protected void init() {
        super.init();
        setFocused(ModularUIClientAccess.getWidget(ui));
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}
}
