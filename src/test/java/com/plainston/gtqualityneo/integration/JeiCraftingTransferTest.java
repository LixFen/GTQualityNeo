package com.plainston.gtqualityneo.integration;

import static org.junit.jupiter.api.Assertions.*;
import gregapi.gui.ContainerCommon;
import gregapi.jei.GT6_JEI_CraftingCategory;
import gregapi.tileentity.tools.MultiTileEntityAdvancedCraftingTable;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.library.load.registration.RecipeTransferRegistration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Proxy;
import java.util.stream.IntStream;

@ExtendWith(EphemeralTestServerProvider.class)
class JeiCraftingTransferTest {
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
    @Test void jeiResolvesBothRecipeHandlersForConcreteWorkbenchMenu() {
        var player = FakePlayerFactory.getMinecraft(server.overworld());
        var table = new MultiTileEntityAdvancedCraftingTable();
        var saved = new CompoundTag();
        saved.putShort("gt.invsize", (short) 71);
        table.readFromNBT2(saved);
        table.setLevel(server.overworld());
        var menu = table.new MultiTileEntityGUICommonAdvancedCraftingTable(player.getInventory(), table, 0);
        var registration = new RecipeTransferRegistration(null, null, null, null);
        new GTQualityJeiPlugin().registerRecipeTransferHandlers(registration);
        var manager = registration.createRecipeTransferManager();
        assertTrue(manager.getRecipeTransferHandler(menu, category(RecipeTypes.CRAFTING)).isPresent());
        assertTrue(manager.getRecipeTransferHandler(menu, category(GT6_JEI_CraftingCategory.TYPE)).isPresent());
        var storageMenu = (ContainerCommon) table.getGUIServer2(1, player);
        assertTrue(manager.getRecipeTransferHandler(storageMenu, category(RecipeTypes.CRAFTING)).isEmpty());
        var transfer = new GTQualityJeiPlugin.TableTransfer<>(RecipeTypes.CRAFTING);
        assertEquals(IntStream.rangeClosed(21, 29).boxed().toList(),
            transfer.getRecipeSlots(menu, null).stream().map(slot -> slot.getSlotIndex()).toList());
        assertEquals(57, transfer.getInventorySlots(menu, null).size());
        assertTrue(transfer.getInventorySlots(menu, null).stream().noneMatch(transfer.getRecipeSlots(menu, null)::contains));
    }
    @SuppressWarnings("unchecked")
    private static <R> IRecipeCategory<R> category(IRecipeType<R> type) {
        return (IRecipeCategory<R>) Proxy.newProxyInstance(IRecipeCategory.class.getClassLoader(), new Class<?>[]{IRecipeCategory.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getRecipeType")) return type;
                throw new AssertionError(method.getName());
            });
    }

    @Test void jeiServerTransferMovesIngredientsFromWorkbenchStorageIntoCraftingGrid() {
        var player = FakePlayerFactory.getMinecraft(server.overworld());
        var table = new MultiTileEntityAdvancedCraftingTable();
        var saved = new CompoundTag();
        saved.putShort("gt.invsize", (short) 71);
        table.readFromNBT2(saved);
        table.setLevel(server.overworld());
        var menu = table.new MultiTileEntityGUICommonAdvancedCraftingTable(player.getInventory(), table, 0);
        var original = player.containerMenu;
        player.containerMenu = menu;
        try {
            table.slot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 4));
            var info = new GTQualityJeiPlugin.TableTransfer<>(RecipeTypes.CRAFTING);
            var crafting = info.getRecipeSlots(menu, null);
            var inventory = info.getInventorySlots(menu, null);
            var source = inventory.stream().filter(slot -> slot.container == table && slot.getSlotIndex() == 0).findFirst().orElseThrow();
            assertTrue(mezz.jei.common.transfer.BasicRecipeTransferHandlerServer.setItemsWithResult(player,
                java.util.List.of(new mezz.jei.common.transfer.TransferOperation(source.index, crafting.getFirst().index)),
                crafting, inventory, false, true));
            assertTrue(table.slot(21).is(net.minecraft.world.item.Items.IRON_INGOT));
            assertEquals(1, table.slot(21).getCount());
            assertEquals(3, table.slot(0).getCount());
        } finally { player.containerMenu = original; }
    }
}
