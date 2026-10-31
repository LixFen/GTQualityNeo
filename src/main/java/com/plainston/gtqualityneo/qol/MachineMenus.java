package com.plainston.gtqualityneo.qol;

import gregapi.gui.ContainerCommonBasicMachine;
import gregapi.tileentity.machines.MultiTileEntityBasicMachine;
import gregapi.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class MachineMenus {
    private MachineMenus() {}

    public static boolean supports(AbstractContainerMenu candidate) {
        return candidate.getClass() == ContainerCommonBasicMachine.class
            && ((ContainerCommonBasicMachine) candidate).mTileEntity instanceof MultiTileEntityBasicMachine machine
            && !(machine instanceof TileEntityBase10MultiBlockMachine);
    }
}
