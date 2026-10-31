package com.plainston.gtqualityneo.qol;

import static org.junit.jupiter.api.Assertions.*;

import gregapi.block.multitileentity.example.MultiTileEntityChest;
import gregapi.gui.ContainerCommon;
import gregapi.gui.ContainerCommonDefault;
import gregapi.tileentity.base.TileEntityBase05Inventories;
import gregapi.tileentity.tools.MultiTileEntityAdvancedCraftingTable;
import gregtech.tileentity.computer.MultiTileEntityHDDSwitch;
import gregtech.tileentity.computer.MultiTileEntityUSBSwitch;
import gregtech.tileentity.energy.storage.MultiTileEntityBatteryBox;
import gregtech.tileentity.energy.storage.MultiTileEntityBatteryBoxLarge;
import gregtech.tileentity.extenders.MultiTileEntityFilter;
import gregtech.tileentity.inventories.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.stream.IntStream;

@ExtendWith(EphemeralTestServerProvider.class)
class StorageMenusTest {
    private static MinecraftServer server;

    @BeforeAll static void initialize(MinecraftServer instance) throws Exception {
        server = instance;
        if (server.overworld() == null) {
            server.getWorldData().overworldData().setInitialized(true);
            var createLevels = MinecraftServer.class.getDeclaredMethod("createLevels");
            createLevels.setAccessible(true);
            server.submit(() -> {
                try { createLevels.invoke(server); }
                catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
            }).get();
        }
    }

    private static <T extends TileEntityBase05Inventories> T inventory(T tile, int size) {
        var saved = new CompoundTag();
        saved.putShort("gt.invsize", (short) size);
        tile.readFromNBT2(saved);
        tile.setLevel(server.overworld());
        return tile;
    }

    @Test void acceptsEveryGt6PlainInventoryFamily() {
        var player = FakePlayerFactory.getMinecraft(server.overworld());
        var tiles = new TileEntityBase05Inventories[] {
            inventory(new MultiTileEntityChest(), 54),
            inventory(new MultiTileEntityHopper(), 1),
            inventory(new MultiTileEntityQueueHopper(), 36),
            inventory(new MultiTileEntityBatteryBox(), 4),
            inventory(new MultiTileEntityBatteryBoxLarge(), 16),
            inventory(new MultiTileEntitySafeMechanical(), 27),
            inventory(new MultiTileEntitySafeKeyLocked(), 27),
            inventory(new MultiTileEntityDrawerQuad(), 144),
            inventory(new MultiTileEntityEnderGarbageBin(), 9),
            inventory(new MultiTileEntityUSBSwitch(), 16),
            inventory(new MultiTileEntityHDDSwitch(), 1)
        };
        for (var tile : tiles) {
            var menu = (ContainerCommon) tile.getGUIServer(0, player);
            try {
                assertTrue(StorageMenus.supports(menu), tile.getClass().getSimpleName());
                assertEquals(menu.mSlotCount + 36, menu.slots.size());
            } finally { menu.removed(player); }
        }
    }

    @Test void drawerPagesKeepOnlyTheirOwnThirtySixSlotsAndShiftClickDestination() {
        var player = FakePlayerFactory.getMinecraft(server.overworld());
        var drawer = inventory(new MultiTileEntityDrawerQuad(), 144);
        for (int page = 0; page < 4; page++) {
            var menu = (ContainerCommon) drawer.getGUIServer( page, player);
            try {
                assertTrue(StorageMenus.supports(menu));
                assertEquals(IntStream.range(page * 36, (page + 1) * 36).boxed().toList(),
                    menu.slots.stream().filter(slot -> slot.container == drawer).map(slot -> slot.getSlotIndex()).toList());
                player.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 12));
                menu.quickMoveStack(player, 36);
                assertTrue(player.getInventory().getItem(9).isEmpty());
                assertEquals(12, drawer.slot(page * 36).getCount());
            } finally { menu.removed(player); }
        }
    }

    @Test void workbenchStorageUsesItsOffsetWhileSpecialMenusRemainUntouched() {
        var player = FakePlayerFactory.getMinecraft(server.overworld());
        var table = inventory(new MultiTileEntityAdvancedCraftingTable(), 71);
        var storage = (ContainerCommon) table.getGUIServer(1, player);
        var crafting = (ContainerCommon) table.getGUIServer(0, player);
        var filter = inventory(new MultiTileEntityFilter(), 9);
        var filtering = (ContainerCommon) filter.getGUIServer(0, player);
        var custom = new ContainerCommonDefault(player.getInventory(), table, 1, 35, 36) {};
        try {
            assertTrue(StorageMenus.supports(storage));
            assertEquals(IntStream.range(35, 71).boxed().toList(),
                storage.slots.stream().filter(slot -> slot.container == table).map(slot -> slot.getSlotIndex()).toList());
            assertFalse(StorageMenus.supports(crafting));
            assertFalse(StorageMenus.supports(filtering));
            assertFalse(StorageMenus.supports(custom));
            assertFalse(StorageMenus.supports(player.inventoryMenu));
        } finally {
            storage.removed(player);
            crafting.removed(player);
            filtering.removed(player);
            custom.removed(player);
        }
    }
}
