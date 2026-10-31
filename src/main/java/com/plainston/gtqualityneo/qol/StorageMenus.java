package com.plainston.gtqualityneo.qol;

import gregapi.gui.ContainerCommon;
import gregapi.gui.ContainerCommonChest;
import gregapi.gui.ContainerCommonDefault;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Only the plain inventory menus; subclasses may implement their own layout and click rules. */
public final class StorageMenus {
    private StorageMenus() {}

    public static boolean supports(AbstractContainerMenu candidate) {
        return candidate instanceof ContainerCommon menu && menu.mTileEntity != null
            && (menu.getClass() == ContainerCommonChest.class || menu.getClass() == ContainerCommonDefault.class);
    }
}
