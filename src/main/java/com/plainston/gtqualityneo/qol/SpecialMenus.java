package com.plainston.gtqualityneo.qol;

import gregapi.tileentity.tools.MultiTileEntityAdvancedCraftingTable.MultiTileEntityGUICommonAdvancedCraftingTable;
import gregtech.tileentity.extenders.MultiTileEntityFilter.MultiTileEntityGUICommonFilter;
import gregtech.tileentity.extenders.MultiTileEntityFilterPrefix.MultiTileEntityGUICommonFilterPrefix;
import gregtech.tileentity.tools.MultiTileEntityBumbliary;
import gregtech.tileentity.tools.MultiTileEntityBumbliaryAdvanced;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** These menus require their original spatial layout and click rules, rather than a storage grid. */
public final class SpecialMenus {
    private SpecialMenus() {}

    public static boolean supports(AbstractContainerMenu menu) {
        return menu instanceof MultiTileEntityGUICommonAdvancedCraftingTable
            || menu instanceof MultiTileEntityGUICommonFilter
            || menu instanceof MultiTileEntityGUICommonFilterPrefix
            || menu instanceof MultiTileEntityBumbliary.MultiTileEntityGUICommonBumbliary
            || menu instanceof MultiTileEntityBumbliary.MultiTileEntityGUICommonBumbliaryScoop
            || menu instanceof MultiTileEntityBumbliaryAdvanced.MultiTileEntityGUICommonBumbliary
            || menu instanceof MultiTileEntityBumbliaryAdvanced.MultiTileEntityGUICommonBumbliaryScoop;
    }
}
