package com.plainston.gtqualityneo.integration.ae2;

import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import com.plainston.gtqualityneo.GTQualityNeo;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/** Loaded only when AE2 is installed. */
public final class AE2Integration {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GTQualityNeo.MOD_ID);
    public static final Supplier<PowderCell> CELL = ITEMS.registerItem("powder_cell", PowderCell::new, properties -> properties.stacksTo(1));
    private AE2Integration() {}
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            Upgrades.add(AEItems.INVERTER_CARD, CELL.get(), 1);
            StorageCells.addCellHandler(new ICellHandler() {
                @Override public boolean isCell(ItemStack stack) { return stack.is(CELL.get()); }
                @Override public StorageCell getCellInventory(ItemStack stack, ISaveProvider host) {
                    return isCell(stack) ? new PowderCellInventory(stack, host) : null;
                }
            });
        }));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(CELL.get());
        });
    }
}
