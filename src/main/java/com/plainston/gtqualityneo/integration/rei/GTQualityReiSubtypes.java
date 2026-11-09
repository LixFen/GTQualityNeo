package com.plainston.gtqualityneo.integration.rei;

import gregapi.GT_API;
import gregapi.data.CS.ModIDs;
import gregapi.item.ItemFluidDisplay;
import gregapi.util.ST;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Objects;

@REIPluginClient
public final class GTQualityReiSubtypes implements REICommonPlugin {
    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        if (GT_API.SUBTYPE_TYPE == null) return;
        for (var item : BuiltInRegistries.ITEM) {
            if (!ModIDs.isGregNamespace(BuiltInRegistries.ITEM.getKey(item).getNamespace())) continue;
            // A fluid icon's meta identifies the fluid; its data only describes volume and tooltip state.
            boolean includeData = !(item instanceof ItemFluidDisplay) && ST.identityIncludesNBT(item);
            registry.register((context, stack) -> {
                long meta = ST.meta_(stack);
                // REI's component(...) takes an exclusion list. Hash the two GT6 identity fields directly.
                return includeData ? (meta << Integer.SIZE) ^ Integer.toUnsignedLong(
                    Objects.hashCode(stack.get(GT_API.DATA_TYPE))) : meta;
            }, item);
        }
    }
}
