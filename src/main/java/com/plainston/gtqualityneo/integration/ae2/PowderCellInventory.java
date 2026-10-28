package com.plainston.gtqualityneo.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.core.definitions.AEItems;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PowderCellInventory implements StorageCell {
    static final String CONTENTS = "gtqualityneo.powders";
    final PowderAmounts amounts = new PowderAmounts();
    private final ItemStack cell;
    private final ISaveProvider host;
    PowderCellInventory(ItemStack cell, ISaveProvider host) {
        this.cell = cell;
        this.host = host;
        amounts.read(cell.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getListOrEmpty(CONTENTS));
    }
    static OreDictItemData powder(ItemStack stack) {
        if (stack.isEmpty()) return null;
        var data = OM.anydata_(stack);
        if (data == null || !data.validData() || !data.mPrefix.contains(TD.Prefix.DUST_BASED) || data.mPrefix.mAmount <= 0) return null;
        if (data.mBlackListed && (data.mMaterial.mMaterial == MT.Glass || !MD.MC.owns(stack))) return null;
        // Only the unmodified material item may be unified. Never discard names or other custom components.
        var canonical = data.mPrefix.mat(data.mMaterial.mMaterial, 1);
        if (canonical.isEmpty() || !ItemStack.isSameItemSameComponents(canonical, stack)
            || ST.invalid(OP.dust.mat(data.mMaterial.mMaterial, 1))) return null;
        return data;
    }
    private ItemStack marked(String material) {
        var config = ((PowderCell) cell.getItem()).getConfigInventory(cell);
        for (int slot = 0; slot < config.size(); slot++) {
            if (!(config.getKey(slot) instanceof AEItemKey key)) continue;
            var stack = key.toStack();
            var data = powder(stack);
            if (data != null && data.mMaterial.mMaterial.mNameInternal.equals(material)) return stack;
        }
        return ItemStack.EMPTY;
    }
    ItemStack output(String material) {
        var marked = marked(material);
        if (!marked.isEmpty()) return marked;
        var mat = OreDictMaterial.MATERIAL_MAP.get(material);
        return mat == null ? ItemStack.EMPTY : OP.dust.mat(mat, 1);
    }
    @Override public long insert(AEKey what, long count, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey key) || count <= 0) return 0;
        var data = powder(key.toStack());
        if (data == null) return 0;
        String material = data.mMaterial.mMaterial.mNameInternal;
        if (((PowderCell) cell.getItem()).getUpgrades(cell).isInstalled(AEItems.INVERTER_CARD) && marked(material).isEmpty()) return 0;
        long moved = amounts.insert(material, data.mPrefix.mAmount, count, mode.isSimulate());
        if (moved > 0 && !mode.isSimulate()) persist();
        return moved;
    }
    @Override public long extract(AEKey what, long count, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey key) || count <= 0) return 0;
        var data = powder(key.toStack());
        if (data == null) return 0;
        String material = data.mMaterial.mMaterial.mNameInternal;
        if (!key.matches(output(material))) return 0;
        long moved = amounts.extract(material, data.mPrefix.mAmount, count, mode.isSimulate());
        if (moved > 0 && !mode.isSimulate()) persist();
        return moved;
    }
    @Override public void getAvailableStacks(KeyCounter out) {
        amounts.materials.forEach((material, units) -> {
            var stack = output(material);
            var data = powder(stack);
            if (data != null && units >= data.mPrefix.mAmount) out.add(AEItemKey.of(stack), units / data.mPrefix.mAmount);
        });
    }
    @Override public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        var data = what instanceof AEItemKey key ? powder(key.toStack()) : null;
        return data != null && amounts.materials.containsKey(data.mMaterial.mMaterial.mNameInternal);
    }
    @Override public CellState getStatus() {
        if (amounts.materials.isEmpty()) return CellState.EMPTY;
        if (amounts.materials.size() < PowderAmounts.MAX_TYPES) return CellState.NOT_EMPTY;
        return amounts.materials.values().stream().allMatch(value -> PowderAmounts.MAX_UNITS - value < gregapi.data.CS.U72)
            ? CellState.FULL : CellState.TYPES_FULL;
    }
    @Override public double getIdleDrain() { return 2; }
    @Override public boolean canFitInsideCell() { return amounts.materials.isEmpty(); }
    @Override public Component getDescription() { return cell.getHoverName(); }
    @Override public void persist() {
        CustomData.update(DataComponents.CUSTOM_DATA, cell, tag -> tag.put(CONTENTS, amounts.write()));
        if (host != null) host.saveChanges();
    }
}
