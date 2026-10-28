package com.plainston.gtqualityneo.integration.ae2;

import static org.junit.jupiter.api.Assertions.*;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import appeng.core.definitions.AEItems;
import gregapi.data.MT;
import gregapi.data.OP;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(EphemeralTestServerProvider.class)
class PowderCellInventoryTest {
    @org.junit.jupiter.api.BeforeAll static void initialize(net.minecraft.server.MinecraftServer server) throws Exception {
        // Loading the level runs GT6's deferred material, recipe and worldgen initialization.
        if (server.overworld() == null) {
            server.getWorldData().overworldData().setInitialized(true);
            var createLevels = net.minecraft.server.MinecraftServer.class.getDeclaredMethod("createLevels");
            createLevels.setAccessible(true);
            server.submit(() -> {
                try { createLevels.invoke(server); }
                catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
            }).get();
        }
    }
    @Test void aeNetworkSeesConvertedOutputAndFractionalRemaindersSurviveReload() {
        var cell = new ItemStack(AE2Integration.CELL.get());
        var saved = new AtomicInteger();
        StorageCell storage = StorageCells.getCellInventory(cell, saved::incrementAndGet);
        assertNotNull(storage);
        var small = AEItemKey.of(OP.dustSmall.mat(MT.Fe, 1));
        var dust = AEItemKey.of(OP.dust.mat(MT.Fe, 1));
        assertEquals(3, storage.insert(small, 3, Actionable.SIMULATE, null));
        assertTrue(storage.getAvailableStacks().isEmpty());
        assertEquals(0, saved.get());
        assertEquals(3, storage.insert(small, 3, Actionable.MODULATE, null));
        assertFalse(storage.canFitInsideCell(), "Hidden fractions still occupy the cell");
        storage = StorageCells.getCellInventory(cell.copy(), null);
        assertEquals(1, storage.insert(small, 1, Actionable.MODULATE, null));
        assertEquals(1, storage.getAvailableStacks().get(dust));
        assertEquals(0, storage.getAvailableStacks().get(small));
        assertEquals(0, storage.extract(small, 4, Actionable.MODULATE, null));
        assertEquals(1, storage.extract(dust, 1, Actionable.MODULATE, null));
        assertTrue(storage.getAvailableStacks().isEmpty());
        assertTrue(storage.canFitInsideCell());
    }
    @Test void workbenchMarksChangeOnlyTheirMaterialAndInverterRestrictsNewInput() {
        var cell = new ItemStack(AE2Integration.CELL.get());
        var item = (PowderCell) cell.getItem();
        var iron = AEItemKey.of(OP.dust.mat(MT.Fe, 1));
        var copper = AEItemKey.of(OP.dust.mat(MT.Cu, 1));
        var tiny = AEItemKey.of(OP.dustTiny.mat(MT.Fe, 1));
        var small = AEItemKey.of(OP.dustSmall.mat(MT.Fe, 1));
        var storage = StorageCells.getCellInventory(cell, null);
        storage.insert(iron, 2, Actionable.MODULATE, null);
        storage.insert(copper, 1, Actionable.MODULATE, null);
        var marks = item.getConfigInventory(cell);
        marks.setStack(0, new GenericStack(tiny, 1));
        marks.setStack(1, new GenericStack(small, 1));
        assertEquals(18, storage.getAvailableStacks().get(tiny));
        assertEquals(1, storage.getAvailableStacks().get(copper));
        assertEquals(0, storage.extract(iron, 1, Actionable.MODULATE, null));
        assertEquals(9, storage.extract(tiny, 9, Actionable.SIMULATE, null));
        assertEquals(18, storage.getAvailableStacks().get(tiny));
        item.getUpgrades(cell).setItemDirect(0, AEItems.INVERTER_CARD.stack());
        assertEquals(0, storage.insert(copper, 1, Actionable.MODULATE, null));
        assertEquals(1, storage.insert(iron, 1, Actionable.MODULATE, null));
        assertEquals(1, storage.extract(copper, 1, Actionable.MODULATE, null), "Previously stored material remains extractable");
        var restored = StorageCells.getCellInventory(cell.copy(), null);
        assertEquals(27, restored.getAvailableStacks().get(tiny));
    }
    @Test void namedPowderIsRejectedWithoutLosingItsComponents() {
        var cell = new ItemStack(AE2Integration.CELL.get());
        var stack = OP.dust.mat(MT.Fe, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Keep me"));
        var storage = StorageCells.getCellInventory(cell, null);
        assertEquals(0, storage.insert(AEItemKey.of(stack), 10, Actionable.MODULATE, null));
        assertEquals(Component.literal("Keep me"), stack.getHoverName());
        assertTrue(storage.canFitInsideCell());
    }
}
