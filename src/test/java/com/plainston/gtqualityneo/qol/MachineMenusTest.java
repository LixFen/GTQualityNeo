package com.plainston.gtqualityneo.qol;

import static org.junit.jupiter.api.Assertions.*;

import com.plainston.gtqualityneo.fluid.GuiFluidInteraction;
import gregapi.data.RM;
import gregapi.gui.ContainerCommonBasicMachine;
import gregapi.gui.Slot_Render;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.tileentity.machines.MultiTileEntityBasicMachine;
import gregtech.tileentity.multiblocks.MultiTileEntityMixer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
class MachineMenusTest {
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

    private static MultiTileEntityBasicMachine machine(RecipeMap recipes) {
        var machine = new MultiTileEntityBasicMachine();
        machine.mRecipes = recipes;
        machine.readFromNBT2(new CompoundTag());
        machine.setLevel(server.overworld());
        return machine;
    }

    @Test void recipeShapesPreserveOutputRestrictionsSpecialIndexesAndFluidInteractionTargets() {
        var player = FakePlayerFactory.getMinecraft(server.overworld());
        for (var recipes : new RecipeMap[]{RM.Furnace, RM.Mixer, RM.Electrolyzer, RM.Centrifuge, RM.Autocrafter}) {
            var machine = machine(recipes);
            var menu = (ContainerCommonBasicMachine) machine.getGUIServer(0, player);
            try {
                assertTrue(MachineMenus.supports(menu));
                assertFalse(StorageMenus.supports(menu));
                int special = recipes.mInputItemsCount + recipes.mOutputItemsCount;
                int fluidSlots = 0;
                for (var slot : menu.slots) {
                    if (slot.container == player.getInventory()) continue;
                    int index = slot.getSlotIndex();
                    if (slot instanceof Slot_Render) {
                        fluidSlots++;
                        assertTrue(GuiFluidInteraction.isFluidSlot(menu, slot.index));
                        assertTrue(index > special);
                    } else {
                        assertFalse(GuiFluidInteraction.isFluidSlot(menu, slot.index));
                        if (index >= recipes.mInputItemsCount && index < special)
                            assertFalse(slot.mayPlace(new ItemStack(Items.IRON_INGOT)));
                    }
                }
                assertEquals(recipes.mInputFluidCount + recipes.mOutputFluidCount, fluidSlots);
                assertEquals(recipes == RM.Autocrafter, menu.slots.stream()
                    .anyMatch(slot -> slot.container == machine && slot.getSlotIndex() == special));
                menu.setData(0, Short.MAX_VALUE / 2);
                assertEquals(Short.MAX_VALUE / 2, menu.mProgressBar);
                menu.setData(0, -1);
                assertEquals(-1, menu.mProgressBar);
            } finally { menu.removed(player); }
        }
    }

    @Test void leavesMultiblocksAndCustomMachineMenusForTheirOwnMigration() {
        var player = FakePlayerFactory.getMinecraft(server.overworld());
        var multi = new MultiTileEntityMixer();
        multi.mRecipes = RM.Mixer;
        multi.readFromNBT2(new CompoundTag());
        multi.setLevel(server.overworld());
        var multiblock = (ContainerCommonBasicMachine) multi.getGUIServer(0, player);
        var machine = machine(RM.Furnace);
        var custom = new ContainerCommonBasicMachine(player.getInventory(), machine, RM.Furnace, 0) {};
        try {
            assertFalse(MachineMenus.supports(multiblock));
            assertFalse(MachineMenus.supports(custom));
            assertFalse(MachineMenus.supports(player.inventoryMenu));
        } finally {
            multiblock.removed(player);
            custom.removed(player);
        }
    }
}
