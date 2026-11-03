package com.plainston.gtqualityneo.client.ui;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import gregapi.data.LH;
import gregapi.gui.ContainerClient;
import gregapi.gui.ContainerCommon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Retains GT6's functional slot arrangement, permissions, and menu click dispatch. */
public final class FixedInventoryScreen extends AbstractContainerScreen<ContainerCommon> {
    private final ModularUI ui;

    FixedInventoryScreen(ContainerClient original) {
        this(original.getMenu(), original.mBackground, new FixedInventoryLayout(original.getMenu()));
    }

    private FixedInventoryScreen(ContainerCommon menu, Identifier texture, FixedInventoryLayout layout) {
        super(menu, menu.mInventoryPlayer, Component.literal(LH.get(menu.mTileEntity.getInventoryNameGUI())),
            FixedInventoryLayout.WIDTH, layout.height());
        var root = InventoryUi.panel(menu, title, imageWidth, imageHeight);
        for (int i = 0; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            var position = layout.position(i);
            var element = InventoryUi.slot(menu, slot, position.x(), position.y());
            if (slot.container != menu.mInventoryPlayer) {
                var source = layout.sources().get(i);
                element.style(s -> s.background(SpriteTexture.of(texture).setSprite(source.x(), source.y(), 18, 18)));
            }
            root.addChild(element);
        }
        ui = ModularUI.of(UI.of(root), menu.mInventoryPlayer.player);
        ((IModularUIHolderMenu) menu).setModularUI(ui);
    }

    @Override protected void init() {
        super.init();
        setFocused(ModularUIClientAccess.getWidget(ui));
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}
}
