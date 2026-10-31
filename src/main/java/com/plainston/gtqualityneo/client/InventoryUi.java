package com.plainston.gtqualityneo.client;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import dev.vfyjxf.taffy.style.TaffyPosition;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.gui.ContainerCommon;
import gregapi.gui.Slot_Base;
import gregapi.gui.Slot_Render;
import gregapi.tileentity.base.TileEntityBase04MultiTileEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

/** Shared vanilla panel, name tab and bindings for the inventory and machine screens. */
final class InventoryUi {
    static final SpriteTexture SLOT = SpriteTexture.of("minecraft:textures/gui/container/generic_54.png")
        .setSprite(7, 17, 18, 18).setBorder(1);

    private InventoryUi() {}

    static UIElement panel(ContainerCommon menu, Component title, int width, int height) {
        var root = new UIElement().layout(l -> l.width(width).height(height));
        var font = Minecraft.getInstance().font;
        var name = Component.literal(font.plainSubstrByWidth(title.getString(), width - 30));
        int tabWidth = Math.min(width, font.width(name) + 30);
        var tab = new UIElement()
            .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).width(tabWidth).height(22))
            .style(s -> s.background(MCSprites.TAB_ON).tooltips(title));
        var tile = (TileEntityBase04MultiTileEntities) menu.mTileEntity;
        var icon = MultiTileEntityRegistry.getRegistry(tile.getMultiTileEntityRegistryID())
            .getItem(tile.getMultiTileEntityID());
        tab.addChild(new UIElement()
            .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(4).top(2).width(16).height(16))
            .style(s -> s.background(new ItemStackTexture(icon))));
        tab.addChild(label(name, 23, 6));
        root.addChild(tab);
        root.addChild(new UIElement()
            .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(StorageLayout.PANEL_Y)
                .width(width).height(height - StorageLayout.PANEL_Y))
            .style(s -> s.background(MCSprites.RECT)));
        return root;
    }

    static UIElement label(Component text, int x, int y) {
        return new Label().setText(text).textStyle(s -> s.textColor(0xff404040).textShadow(false))
            .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y));
    }

    static ItemSlot slot(ContainerCommon menu, Slot slot, int x, int y) {
        var element = new ItemSlot().bind(slot)
            .slotStyle(s -> s.isPlayerSlot(slot.container == menu.mInventoryPlayer));
        element.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y)
            .width(18).height(18).paddingAll(1));
        element.style(s -> s.background(SLOT));
        if (slot instanceof Slot_Base gtSlot && !(slot instanceof Slot_Render)) {
            var tips = gtSlot.getTooltip(menu.mInventoryPlayer.player, Minecraft.getInstance().options.advancedItemTooltips);
            element.style(s -> s.tooltips(tips.stream().map(Component::literal).toArray(Component[]::new)));
        }
        // Register existing slots for LDLib rendering without adding them to the menu again.
        ((IModularUIHolderMenu) menu).ldlib2$addSlot(element);
        return element;
    }
}
