package com.plainston.gtqualityneo.integration.ae2;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.items.contents.CellConfig;
import appeng.util.ConfigInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.Set;
import java.util.function.Consumer;

public final class PowderCell extends Item implements ICellWorkbenchItem {
    public PowderCell(Properties properties) { super(properties); }
    @Override public ConfigInventory getConfigInventory(ItemStack stack) { return CellConfig.create(Set.of(AEKeyType.items()), stack); }
    @Override public IUpgradeInventory getUpgrades(ItemStack stack) { return UpgradeInventories.forItem(stack, 1); }
    @Override public FuzzyMode getFuzzyMode(ItemStack stack) { return FuzzyMode.IGNORE_ALL; }
    @Override public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {}
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("gtqualityneo.powder_cell.description"));
        tooltip.accept(Component.translatable("gtqualityneo.powder_cell.capacity", PowderAmounts.MAX_TYPES));
        tooltip.accept(Component.translatable("gtqualityneo.powder_cell.workbench"));
        tooltip.accept(Component.translatable("gtqualityneo.powder_cell.inverter"));
        var inventory = new PowderCellInventory(stack, null);
        tooltip.accept(Component.translatable("gtqualityneo.powder_cell.types", inventory.amounts.materials.size()));
        inventory.amounts.materials.forEach((material, units) -> {
            var output = inventory.output(material);
            var data = PowderCellInventory.powder(output);
            if (data == null) return;
            long size = data.mPrefix.mAmount;
            long remainder = units % size;
            if (remainder > 0) tooltip.accept(Component.translatable("gtqualityneo.powder_cell.remainder", output.getHoverName(),
                remainder / gregapi.data.CS.U72, size / gregapi.data.CS.U72));
        });
    }
}
