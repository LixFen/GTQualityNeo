package com.plainston.gtqualityneo.client.ui;

import gregapi.gui.ContainerClient;
import com.plainston.gtqualityneo.qol.StorageMenus;
import com.plainston.gtqualityneo.qol.MachineMenus;
import gregapi.gui.ContainerCommonBasicMachine;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client UI integration that keeps GT6's original menu and slot identities. */
public final class StorageUiIntegration {
    private StorageUiIntegration() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(StorageUiIntegration::opening);
    }

    private static void opening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof ContainerClient screen)) return;
        var menu = screen.getMenu();
        if (StorageMenus.supports(menu)) event.setNewScreen(new StorageScreen(menu));
        else if (MachineMenus.supports(menu))
            event.setNewScreen(new MachineScreen((ContainerCommonBasicMachine) menu));
    }
}
