package com.plainston.gtqualityneo.qol;

import gregapi.gui.ContainerCommon;
import gregapi.gui.Slot_Holo;
import gregtech.tileentity.extenders.MultiTileEntityFilter.MultiTileEntityGUICommonFilter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public final class FilterGhost {
    private FilterGhost() {}
    public static boolean isFilterSlot(AbstractContainerMenu menu, int slot) {
        return menu instanceof MultiTileEntityGUICommonFilter
            && slot >= 0 && slot < menu.slots.size() && menu.slots.get(slot) instanceof Slot_Holo;
    }
    public static boolean select(ServerPlayer player, int window, int slot, ItemStack stack) {
        var menu = player.containerMenu;
        if (menu.containerId != window || !isFilterSlot(menu, slot) || stack.isEmpty()
            || !menu.stillValid(player) || !(((ContainerCommon) menu).mTileEntity instanceof net.minecraft.world.level.block.entity.BlockEntity tile)
            || !player.level().hasChunkAt(tile.getBlockPos()) || !player.level().mayInteract(player, tile.getBlockPos())) return false;
        var held = menu.getCarried();
        var ghost = stack.copyWithCount(1);
        if (menu instanceof MultiTileEntityGUICommonFilter
            && ((ContainerCommon) menu).mTileEntity instanceof gregtech.tileentity.extenders.MultiTileEntityFilter filter
            && (filter.mModes & gregapi.tileentity.delegate.ITileEntityDelegating.EXTENDER_INV) == 0) {
            // Amount-zero GT6 display stacks become EMPTY FluidStacks in NeoForge. Resolve their type explicitly.
            var fluid = gregapi.data.IL.Display_Fluid.equal(ghost, true, true)
                ? gregapi.data.FL.make(gregapi.data.FL.fluid(gregapi.util.ST.meta_(ghost)), 1)
                : net.neoforged.neoforge.transfer.fluid.FluidUtil.getFirstStackContained(ghost);
            if (fluid != null && !fluid.isEmpty()) ghost = gregapi.data.FL.display(fluid.copyWithAmount(1), false, false);
        }
        if (ghost == null || ghost.isEmpty()) return false;
        menu.setCarried(ghost);
        try { menu.clicked(slot, 0, ContainerInput.PICKUP, player); }
        finally { menu.setCarried(held); }
        ((ContainerCommon) menu).mTileEntity.markDirtyGUI();
        menu.broadcastChanges();
        return true;
    }
}
