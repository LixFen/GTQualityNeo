package com.plainston.gtqualityneo.integration.rei;

import gregapi.gui.Slot_Base;
import gregapi.tileentity.tools.MultiTileEntityAdvancedCraftingTable.MultiTileEntityGUICommonAdvancedCraftingTable;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import net.minecraft.world.inventory.Slot;
import java.util.Comparator;

final class ReiTableTransfer implements SimpleTransferHandler {
    private static final CategoryIdentifier<?> CRAFTING = CategoryIdentifier.of("minecraft", "plugins/crafting");

    @Override public ApplicabilityResult checkApplicable(Context context) {
        return context.getMenu() instanceof MultiTileEntityGUICommonAdvancedCraftingTable
            && CRAFTING.equals(context.getDisplay().getCategoryIdentifier())
            && context.getContainerScreen() != null
            ? ApplicabilityResult.createApplicable() : ApplicabilityResult.createNotApplicable();
    }

    @Override public Iterable<SlotAccessor> getInputSlots(Context context) {
        var menu = (MultiTileEntityGUICommonAdvancedCraftingTable) context.getMenu();
        return menu.slots.stream().filter(slot -> slot instanceof Slot_Base base && base.mInventory == menu.mTileEntity
                && slot.getSlotIndex() >= 21 && slot.getSlotIndex() <= 29)
            .sorted(Comparator.comparingInt(Slot::getSlotIndex)).map(SlotAccessor::fromSlot).toList();
    }

    @Override public Iterable<SlotAccessor> getInventorySlots(Context context) {
        var menu = (MultiTileEntityGUICommonAdvancedCraftingTable) context.getMenu();
        return menu.slots.stream().filter(slot -> slot.container == menu.mInventoryPlayer
                || slot instanceof Slot_Base base && base.mInventory == menu.mTileEntity && slot.getSlotIndex() <= 20)
            .map(SlotAccessor::fromSlot).toList();
    }
}
